# Lista de Tareas (Backlog): Odoo Móvil

## 1. Módulo de Asistencia (Core)
- [x] 1.1. Implementación de login dinámico (URL/DB).
- [x] 1.2. Flujo de Check-in con escaneo de QR.
- [x] 1.3. Flujo de Check-out con obtención de ID de asistencia abierta.
- [x] 1.4. Captura de Latitud/Longitud e IP en cada registro.
- [x] 1.5. Geocodificación inversa para obtener Ciudad/País.
- [ ] 1.6. **Tarea**: Implementar validación de horario para evitar doble Check-in accidental.

## 2. Interfaz y Navegación
- [x] 2.1. Separación de Login y MainActivity.
- [x] 2.2. Implementación de Sidebar (Navigation Drawer).
- [x] 2.3. Implementación de Curved Bottom Navigation Bar.
- [x] 2.4. Personalización de colores corporativos (Azul/Celeste).
- [ ] 2.5. **Tarea**: Implementar Dark Mode automático según el sistema.

## 3. Módulo de Administración (Rol Admin)
- [x] 3.1. Verificación de permisos mediante `has_group`.
- [x] 3.2. Reporte de personal en tiempo real (Iconos de presencia).
- [x] 3.3. Optimización de imágenes en reportes mediante LruCache.
- [x] 3.4. Generación de Boleta de Pago Semanal en formato imagen.
- [x] 3.5. Funcionalidad de compartir boleta por WhatsApp.
- [ ] 3.6. **Tarea**: Añadir buscador por nombre en la lista de Reporte de Personal.
- [ ] 3.7. **Tarea**: Implementar filtro por "Semana Anterior" en el reporte de personal.

## 4. Módulo de Empleado (Rol User)
- [x] 4.1. Visualización de últimas asistencias de la semana actual.
- [x] 4.2. Visualización de horas trabajadas e importe ganado por turno.
- [x] 4.3. Pantalla de Perfil (lectura de Odoo).
- [x] 4.4. Pantalla "Mi Contrato" (Wage, Hourly Wage, State).
- [ ] 4.5. **Tarea**: Permitir edición de Teléfono y Email desde la App.

## 5. Funcionalidades Avanzadas (Futuro)
- [ ] 5.1. **Tarea**: Implementar Geofencing (validar radio de 100m de la empresa).
- [ ] 5.2. **Tarea**: Implementar base de datos local (Room) para soporte offline.
- [ ] 5.3. **Tarea**: Añadir login biométrico.
- [ ] 5.4. **Tarea**: Exportación de reportes a PDF/Excel.
