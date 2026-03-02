## 资源包基础结构
你的资源包名称/
├── pack.mcmeta          # 资源包配置文件
├── pack.png             # 资源包图标（可选）
└── assets/
    └── show_damage/     # 必须为此命名空间
        └── font/
            └── damage_number.json    # 字体定义文件
        └── textures/
            └── font/                 # 字体纹理目录（如使用位图字体）   
---
## 创建 pack.mcmeta
{
    "pack": {
        "pack_format": 15,
        "description": "Show Damage 自定义字体包 - 示例"
    }
}
---
## 放置字体文件
### 对于 TTF 方案：
assets/
└── show_damage/
    └── font/
        ├── damage_number.json      # 字体定义
        └── damage_number.ttf       # TTF 字体文件

---
### 对于位图方案：
assets/
└── show_damage/
    ├── font/
    │   └── damage_number.json      # 字体定义
    └── textures/
        └── font/
            └── damage_number.png   # 位图纹理

## 游戏内配置
安装资源包后，在模组配置界面（Mods → Show Damage → Config）中
| 配置项                 | 说明                                |
| ------------------- | --------------------------------- |
| **Use Custom Font** | 启用/禁用自定义字体                        |
| **Forced Font**     | 强制使用特定字体（当 Use Custom Font 关闭时生效） |

1. 将资源包放入 .minecraft/resourcepacks/
2. 游戏内 选项 → 资源包 中启用该包
3. 按 F3+T 重载资源（或重启游戏）


