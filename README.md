# ⚔️ RPG Levels - Minecraft NeoForge Mod (1.21.1)

Mod de rol y progresión para **Minecraft 1.21.1** desarrollado sobre **NeoForge**. Incluye sistemas de Rango de Aventureros, Tarjeta de Gremio, Sliders de Control de Poder, Técnicas Espirituales, Cristales de Alma, Contratos y Bounties Diarios, Reinos y Aldeas procedurales, y la dimensión del Abismo.

---

## 📋 Requisitos Previos

Antes de comenzar a trabajar en el proyecto, asegúrate de tener instalado en tu computadora:

1. **Java 21 JDK**:
   - Descarga e instala [Eclipse Temurin 21 (LTS)](https://adoptium.net/temurin/releases/?version=21) o Microsoft OpenJDK 21.
   - Asegúrate de tener la variable de entorno `JAVA_HOME` apuntando al JDK 21 y agregada a tu `Path`.
   - Verifica en terminal con:
     ```bash
     java -version
     ```
2. **Git**:
   - Descarga e instala [Git para Windows](https://git-scm.com/).
3. **Google Antigravity IDE**:
   - El entorno de desarrollo con IA para programar, compilar y probar.

---

## 🚀 Guía de Inicio Rápido (Setup Inicial)

### 1. Clonar el repositorio
Abre una terminal (PowerShell o Git Bash) en la carpeta donde guardas tus proyectos:
```bash
git clone https://github.com/PabloGRuiz/minecraft-mod.git
cd minecraft-mod
```

### 2. Abrir en Antigravity
Abre Antigravity y selecciona la carpeta clonada (`minecraft-mod`) como tu espacio de trabajo.

### 3. Mods complementarios de prueba (`run/mods`)
Para no saturar el repositorio de Git, los mods externos pesados (Cataclysm, Iron's Spells, CTOV, Better Combat, Sodium, etc.) están excluidos del repositorio.
- Pídele a Pablo el archivo `MODS_ALPHA_AMIGOS.zip`.
- Crea la carpeta `run/mods` dentro del proyecto si no existe.
- Descomprime todos los archivos `.jar` del zip dentro de `run/mods/`.
*(Esto te permitirá probar el mod en un entorno con todas las estructuras, aldeas ampliadas y animaciones de combate).*

### 4. Compilar por primera vez
Para verificar que el entorno descarga las dependencias de NeoForge y compila sin errores:
```bash
./gradlew compileJava
```

### 5. Probar el juego (Cliente de desarrollo)
Para abrir Minecraft con el mod cargado:
```bash
./gradlew runClient
```

---

## 🤖 Prompt de Arranque para tu Agente de Antigravity

Copia y pega el siguiente mensaje a tu agente de **Antigravity** en tu primer turno de chat. Esto le dará todo el contexto del mod, las convenciones de código y le pedirá verificar el entorno:

```text
¡Hola Antigravity! Voy a trabajar en conjunto en el desarrollo de este mod de Minecraft llamado "RPG Levels".
El mod está desarrollado en Java 21 sobre NeoForge 1.21.1.

Por favor realiza lo siguiente:
1. Inspecciona la estructura del proyecto y familiarízate con las clases en src/main/java/com/pablo/rpglevels/ (sistemas de RPG, datos en ModAttachments, entidades, eventos de renderizado de ki, misiones y generación de reinos).
2. Ejecuta un `./gradlew compileJava` en la terminal para verificar que el entorno de desarrollo y las librerías de NeoForge estén en orden y sin errores de compilación.
3. Confírmame qué herramientas y estado general tiene el mod para que podamos empezar a programar nuevas características.
```

---

## 🔀 Flujo de Trabajo en Equipo con Git (Para no pisarse)

Para que ambos puedan avanzar en paralelo de forma ordenada y sin conflictos de código:

### 1. Siempre sincronizar antes de empezar a trabajar
Antes de pedirle algo nuevo a tu Antigravity, descarga los últimos cambios de la rama principal:
```bash
git checkout main
git pull origin main
```

### 2. Crear una rama para tu tarea (Branching)
Nunca trabajen directamente sobre `main` a la vez. Crea una rama descriptiva para lo que vayas a implementar:
```bash
git checkout -b feature/nombre-de-tu-tarea
# Ejemplo: git checkout -b feature/nuevas-estructuras
```

### 3. Desarrollar con Antigravity
Pídele a Antigravity los cambios, pruebas o nuevas funciones que quieras agregar.

### 4. Guardar tus cambios (Commit & Push)
Cuando una característica esté lista y compiles con éxito (`./gradlew compileJava`):
```bash
git add .
git commit -m "Descripción clara de lo que hiciste"
git push origin feature/nombre-de-tu-tarea
```

### 5. Unir los cambios (Pull Request o Merge)
Entren a [GitHub](https://github.com/PabloGRuiz/minecraft-mod) y abran una **Pull Request** para revisar los cambios juntos y unirlos a `main`.

---

¡A disfrutar programando y creando juntos! 🎮✨
