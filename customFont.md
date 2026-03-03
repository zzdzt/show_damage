## 资源包基础结构 / Resource Pack Basic Structure
    你的资源包名称 / Your resource pack name
    ├── pack.mcmeta          # 资源包配置文件/Resource pack configuration file
    ├── pack.png             # 资源包图标（可选）/ Resource pack icon (optional)
    └── assets/
        └── show_damage/     # 必须为此命名空间 /  Namespace must be exactly this
            └── font/
                └── damage_number.json    # 字体定义文件 / Font definition file
            └── textures/
                └── font/                 # 字体纹理目录（如使用位图字体）/ Font texture directory (e.g., for bitmap fonts)
---
## 创建 pack.mcmeta / create pack.mcmeta
    {
        "pack": {
            "pack_format": 15,
            "description": "Show Damage 自定义字体包 - 示例" 
        }
    }
---
## 放置字体文件 / Placing Font Files
### 对于 TTF 方案： / For TTF-based fonts:
    assets/
    └── show_damage/
        └── font/
            ├── damage_number.json      # 字体定义 / Font definition file
            └── damage_number.ttf       # TTF 字体文件 / TTF font file

---
### 对于位图方案：/ For bitmap-based fonts:
    assets/
    └── show_damage/
        ├── font/
        │   └── damage_number.json      # 字体定义 / Font definition file
        └── textures/
            └── font/
                └── damage_number.png   # 位图纹理 / Bitmap texture

## 游戏内配置 / In-Game Configuration
安装资源包后，在模组配置界面（Mods → Show Damage → Config）中
| 配置项                 | 说明                                |
| ------------------- | --------------------------------- |
| **Use Custom Font** | 启用/禁用自定义字体                        |
| **Forced Font**     | 强制使用特定字体（当 Use Custom Font 关闭时生效） |

1. 将资源包放入 .minecraft/resourcepacks/
2. 游戏内 选项 → 资源包 中启用该包
3. 按 F3+T 重载资源（或重启游戏）

---
After installing the resource pack, go to the mod configuration menu (Mods → Show Damage → Config) and adjust the following settings:

| Configuration Option     | Description                                                                 |
| ------------------------ | --------------------------------------------------------------------------- |
| **Use Custom Font**      | Enable or disable the custom font defined in your resource pack.            |
| **Forced Font**          | Force a specific font name (only takes effect when Use Custom Font is disabled). |

Setup Steps:
1. Place your resource pack folder into .minecraft/resourcepacks/.
2. In-game, go to Options → Resource Packs and enable your pack.
3. Press F3+T to reload resources (or restart the game).

---
