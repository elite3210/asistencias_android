# Plan de Proyecto: Odoo Móvil

## 1. Arquitectura del Proyecto
Se utiliza una arquitectura basada en **MVVM (Model-View-ViewModel)** para asegurar la escalabilidad y facilidad de pruebas.
- **Capa de Red (Network)**: Centralizada en `OdooClient` y `AttendanceRepository`. Uso de Retrofit 2 y JSON-RPC.
- **Capa de Datos (Models)**: Data classes de Kotlin con `@Serializable` para mapeo directo de JSON.
- **Capa de UI**: Actividades y Fragmentos que utilizan **View Binding** para la interacción con los elementos visuales.

## 2. Fases de Desarrollo

### Fase 1: Cimiento y Core (Completada)
- Configuración de entorno Kotlin 2.0 y Gradle.
- Implementación de cliente JSON-RPC dinámico.
- Flujo de Login, Check-in y Check-out.
- Integración de escaneo QR y geolocalización.

### Fase 2: Administración y Reportes (En Curso)
- Implementación de roles (Admin/User).
- Reporte en tiempo real de asistencia del personal.
- Generación de boletas de pago semanales en formato imagen compartible.
- **Próximamente**: Filtros avanzados en reportes y exportación masiva.

### Fase 3: Módulos de RR.HH. Avanzados (Pendiente)
- **Perfil de Empleado**: Visualización y edición limitada de datos personales.
- **Contratos**: Detalle de condiciones laborales y bonos.
- **Notificaciones**: Avisos de entrada/salida y recordatorios.

### Fase 4: Optimización y Seguridad (Pendiente)
- **Geofencing**: Restricción de marca de asistencia por radio GPS.
- **Caché Offline**: Capacidad de registrar marcas sin internet y sincronizar luego.
- **Biometría**: Acceso mediante huella dactilar o reconocimiento facial.

## 3. Hoja de Ruta Inmediata
1. Estabilizar la carga de imágenes en reportes (Caché implementado).
2. Implementar buscador en el selector de productos (Completado).
3. Desarrollar la vista detallada de "Mi Contrato".
4. Añadir validaciones de horario (evitar marcas dobles accidentales).
