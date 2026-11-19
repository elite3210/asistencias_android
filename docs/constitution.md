# Acta de Constitución del Proyecto: Odoo Móvil

## 1. Visión del Proyecto
Desarrollar una aplicación móvil multiplataforma (enfocada en Android) que actúe como un cliente dinámico para el sistema Odoo 18. La aplicación permite el control de asistencia (Check-in/Check-out) de empleados mediante la validación de códigos QR físicos, captura de geolocalización e IP, y gestión de roles administrativos.

## 2. Propósito y Objetivos
- **Universalidad**: Capacidad de conectarse a cualquier servidor Odoo 18 configurando URL y Base de Datos.
- **Seguridad**: Validar la presencia física del empleado mediante QR y GPS.
- **Transparencia**: Permitir a los empleados visualizar su historial semanal, horas trabajadas y cálculo de pago estimado en tiempo real.
- **Gestión**: Facilitar a los administradores el monitoreo de personal y generación de boletas de pago compartibles.

## 3. Stakeholders
- **Desarrollador (IA/Humano)**: Responsable de la implementación técnica y mantenimiento de estándares.
- **Administrador de Odoo**: Configura los parámetros del servidor y gestiona empleados/contratos.
- **Usuario Final (Empleado)**: Registra su jornada diaria.

## 4. Reglas para el Desarrollo Asistido por IA
- **Consistencia Lingüística**: El código y comentarios deben estar en inglés/español según el estándar actual. La interfaz de usuario debe estar en español (Perú).
- **Arquitectura**: Seguir el patrón MVVM y separación de responsabilidades (Network, UI, Models).
- **Seguridad de Datos**: No incluir credenciales reales en el código. Usar `EncryptedSharedPreferences` para persistencia.
- **Manejo de Errores**: Todo proceso de red debe incluir reportes visuales (Toasts/Dialogs) para el diagnóstico.
