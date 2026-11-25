-- =====================================================================
--  TIENDA ALICT · estrellas de las apps (Supabase)
--  Pega TODO este archivo en Supabase > SQL Editor > New query > Run.
--  Se puede ejecutar más de una vez sin problema.
--  No toca nada de "Zonas a evitar": solo agrega una tabla y 3 funciones.
-- =====================================================================

-- 1) Tabla: una calificación por teléfono y por app ---------------------
create table if not exists public.calificaciones (
  app       text not null check (app ~ '^[a-z0-9][a-z0-9-]{0,39}$'),
  device    text not null check (char_length(device) between 8 and 64),
  estrellas smallint not null check (estrellas between 1 and 5),
  ip        text,
  creado    timestamptz not null default now(),
  cambiado  timestamptz not null default now(),
  primary key (app, device)
);
create index if not exists calificaciones_ip     on public.calificaciones (ip, creado);
create index if not exists calificaciones_device on public.calificaciones (device, creado);

-- 2) Seguridad: nadie toca la tabla directo (solo por las funciones) -----
alter table public.calificaciones enable row level security;   -- sin políticas = cerrada
revoke all on public.calificaciones from anon, authenticated;

-- 3) Funciones ---------------------------------------------------------
create or replace function public.ip_cliente() returns text
language plpgsql stable as $$
declare h json; v text;
begin
  begin
    h := nullif(current_setting('request.headers', true), '')::json;
    v := coalesce(h ->> 'cf-connecting-ip', split_part(coalesce(h ->> 'x-forwarded-for', ''), ',', 1));
  exception when others then
    v := '';
  end;
  return left(coalesce(nullif(trim(v), ''), ''), 64);
end $$;

-- Guarda (o cambia) la calificación de un teléfono y devuelve el promedio.
create or replace function public.calificar_app(p_app text, p_device text, p_estrellas integer)
returns json
language plpgsql security definer set search_path = public as $$
declare
  v_ip   text := public.ip_cliente();
  v_n    integer;
  v_prom numeric;
  v_tot  bigint;
begin
  if p_app is null or p_app !~ '^[a-z0-9][a-z0-9-]{0,39}$' then
    raise exception 'App no válida';
  end if;
  if p_device is null or char_length(p_device) < 8 or char_length(p_device) > 64 then
    raise exception 'Dispositivo no válido';
  end if;
  if p_estrellas is null or p_estrellas < 1 or p_estrellas > 5 then
    raise exception 'Las estrellas van de 1 a 5';
  end if;

  -- Un teléfono no puede calificar más de 20 apps distintas por día.
  if not exists (select 1 from public.calificaciones where app = p_app and device = p_device) then
    select count(*) into v_n from public.calificaciones
     where device = p_device and creado > now() - interval '24 hours';
    if v_n >= 20 then
      raise exception 'Llegaste al límite de calificaciones de hoy. Intenta mañana.';
    end if;
    -- Y desde una misma conexión no se aceptan más de 60 calificaciones nuevas por día.
    if v_ip <> '' then
      select count(*) into v_n from public.calificaciones
       where ip = v_ip and creado > now() - interval '24 hours';
      if v_n >= 60 then
        raise exception 'Demasiadas calificaciones desde esta conexión hoy. Intenta mañana.';
      end if;
    end if;
  end if;

  insert into public.calificaciones (app, device, estrellas, ip)
  values (p_app, p_device, p_estrellas::smallint, nullif(v_ip, ''))
  on conflict (app, device)
  do update set estrellas = excluded.estrellas, cambiado = now();

  select round(avg(estrellas)::numeric, 2), count(*) into v_prom, v_tot
    from public.calificaciones where app = p_app;

  return json_build_object('app', p_app, 'promedio', v_prom, 'total', v_tot);
end $$;

-- Promedio y total de cada app (lo que muestra la tienda).
create or replace function public.resumen_calificaciones()
returns table (app text, promedio numeric, total bigint)
language sql stable security definer set search_path = public as $$
  select c.app, round(avg(c.estrellas)::numeric, 2), count(*)
    from public.calificaciones c
   group by c.app
   order by c.app;
$$;

revoke all on function public.calificar_app(text, text, integer) from public;
revoke all on function public.resumen_calificaciones()          from public;
grant execute on function public.calificar_app(text, text, integer) to anon, authenticated;
grant execute on function public.resumen_calificaciones()          to anon, authenticated;
