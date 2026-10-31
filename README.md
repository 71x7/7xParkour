# 7xParkour
![CI](https://github.com/71x7/7xParkour/actions/workflows/build.yml/badge.svg)




![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2025-ED8B00?logo=openjdk&logoColor=white)




![Paper](https://img.shields.io/badge/Paper-1.20%20--%2026.x-00AAFF?logo=papermc&logoColor=white)




![Minecraft](https://img.shields.io/badge/Minecraft-1.20%20--%2026.x-62B47A?logo=minecraft&logoColor=white)




![License](https://img.shields.io/github/license/71x7/7xParkour)

Plugin de parkours para Paper 1.20 - 26.x (Java 17 / 21 / 25).

## Compilar

```bash
unzip 7xParkour.zip
cd 7xParkour
mvn clean package
```

El jar queda en `target/7xParkour-1.0.0.jar`. Requiere JDK 17+ y Maven.

## Instalar

Copiar el jar a `plugins/` y reiniciar el servidor.

## Comandos

`/pk help` muestra todos los comandos.

| Comando | Permiso |
|---|---|
| `/pk create <nombre>` | `xparkour.admin` |
| `/pk setstart <parkour>` | `xparkour.admin` |
| `/pk addcp <parkour>` | `xparkour.admin` |
| `/pk setend <parkour>` | `xparkour.admin` |
| `/pk join <parkour>` | `xparkour.play` |
| `/pk top <parkour>` | `xparkour.play` |

## Licencia

MIT
