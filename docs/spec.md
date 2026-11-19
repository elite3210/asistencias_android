# Especificaciones Técnicas: Odoo Móvil

## 1. Conectividad y Red
- **Protocolo**: JSON-RPC 2.0 sobre HTTPS (POST).
- **Librerías**: Retrofit 2 para el cliente REST, OkHttp para el manejo de cookies y logs.
- **Serialización**: Kotlinx Serialization.
- **Endpoints**:
    - `/web/session/authenticate`: Autenticación y obtención de `session_id`.
    - `/web/dataset/call_kw`: Ejecución de métodos en modelos de Odoo.
    - `/web/webclient/version_info`: Validación de existencia y respuesta del servidor.

## 2. Modelos de Datos de Odoo
- **`hr.attendance`**:
    - `check_in`, `check_out`: Timestamps (UTC).
    - `worked_hours`: Horas calculadas (Double).
    - `in_latitude`, `in_longitude`, `out_latitude`, `out_longitude`: Coordenadas GPS.
    - `in_ip_address`, `out_ip_address`: IP pública capturada.
    - `in_city`, `in_country_name`: Datos de geocodificación inversa.
    - `in_mode`, `out_mode`: Fijado en `"manual"` para registros móviles.
    - `in_browser`: Reutilizado para guardar el string de la semana (ej: "Semana 17 - 2026").
    - `out_browser`: Reutilizado para guardar el nombre del producto fabricado.
- **`hr.employee`**:
    - `name`, `work_email`, `work_phone`, `job_id`, `department_id`, `image_128/1920`.
- **`hr.contract`**:
    - `wage`, `hourly_wage`. Filtro: `state = 'open'`.
- **`res.users`**:
    - Método `has_group` con `"base.group_system"` para determinar rol Admin.
- **`product.product`**:
    - `display_name`, `default_code`.

## 3. Lógica de Negocio
- **Zona Horaria**: Conversión bidireccional entre UTC (servidor) y America/Lima (GMT-5).
- **Calendario**: La semana comienza el lunes (`Calendar.MONDAY`).
- **Moneda**: Sol Peruano (S/).
- **Seguridad**:
    - Validación de QR contra `qr_secret` configurado.
    - Captura de ubicación obligatoria (si el permiso es concedido).
    - Persistencia cifrada de sesión mediante `EncryptedSharedPreferences`.

## 4. UI/UX
- **Diseño**: Material Design 3 con barra inferior curva y FAB central para escaneo.
- **Navegación**: Navigation Drawer (Sidebar) para secciones secundarias y Bottom Navigation para principales.
- **Reportes**: Visualización de últimas asistencias filtradas por la semana actual del dispositivo.
- **Tickets**: Generación de imagen PNG de boleta semanal mediante `Canvas` de Android para compartir en redes sociales.
