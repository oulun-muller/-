# Context: Legion 串流设置界面

## Goal

只还原 Figma 里的设置主容器，不还原外部游戏背景、右侧工具栏等示意内容。

Figma 文件：
`https://www.figma.com/design/8k5xfE5QoG2dnthZiycOoe/Legion-zone-X-%E5%BA%94%E7%94%A8%E5%AE%9D-%E4%B8%B2%E6%B5%81`

当前主要参考节点：

- `2709:17532`：解除实例后的 `主框架2`
- `2709:17568`：键盘透明度模块里的 `素材`
- `2709:17734`：模拟手柄透明度模块里的 `素材`
- `2709:17536`：`icon/关闭/大`
- `2709:17548` / `2709:17554`：`icon/箭头/右`
- `2713:19312`：画面设置对应的两个二级页面

## Implementation

项目是原生 Android Java，核心文件：

- `app/src/main/java/com/legion/streamsettings/MainActivity.java`

当前实现结构：

- `SettingsSurface`：页面背景和主容器定位。
- `SettingsPanel`：设置主框架。
- `StretchScrollView`：内容区域滚动，支持边界拉伸回弹。
- `PanelScrollBar`：真实滚动条，滚动时淡入，停止 1.5s 后淡出。
- `DisplayCard`：画面设置模块。
- `OpacitySlider`：可拖动透明度滑杆，0%-100%，同步更新数字和素材透明度。
- `showOptionPage` / `optionRow`：画面设置二级列表页。
- `BitratePicker`：自定义码率弹窗里的原生 Canvas 滚动选择器。

主容器约束：

- 固定宽度 `300dp`
- 居右
- 距屏幕右边缘 `8dp`
- 高度为屏幕高度减 `16dp`，即上下各留 `8dp`
- 顶部导航固定，只有导航下方内容区域滚动

## Assets

当前保留资源：

- `app/src/main/res/drawable-nodpi/keyboard_material.png`
  - 来自 Figma `2709:17568`，命名为「素材」的整层 PNG
  - 尺寸 `276 x 160`
- `app/src/main/res/drawable-nodpi/gamepad_material.png`
  - 来自 Figma `2709:17734`，命名为「素材」的整层 PNG
  - 尺寸 `276 x 160`
- `app/src/main/res/drawable-nodpi/overlay_keyboard_bg.png`
  - 键盘透明度模块背景 PNG
- `app/src/main/res/drawable-nodpi/overlay_gamepad_bg.png`
  - 模拟手柄透明度模块背景 PNG
- `app/src/main/res/drawable/ic_close_large.xml`
  - Android VectorDrawable
  - 来自 Figma `icon/关闭/大` 图层，不从内部 Union 子级切
  - 外层交互热区是 `24dp`，真实 icon 容器是 `16dp`
- `app/src/main/res/drawable/ic_arrow_right.xml`
  - Android VectorDrawable
  - 来自 Figma `icon/箭头/右` 图层，不从内部 Vector 子级切

已清理旧资源：

- `keyboard_preview.png`
- `gamepad_preview.png`
- `settings_home_figma.png`
- `app/src/main/res/.DS_Store`

## Visual Notes

- 关闭和跳转箭头不要用文本字符，必须使用 Android VectorDrawable。
- icon 切图层级从带 `icon` 的图层开始，不继续看内部子级。
- 透明度模块前景素材必须用 Figma 里命名为「素材」的整层 PNG。
- 透明度模块背景和前景分开：背景使用 `overlay_*_bg.png`，前景使用 `*_material.png`。
- 主框架边框通过顶层 `PanelBorder` 绘制，避免底部边框被内部渐隐或内容遮住。
- 滑杆仍是原生 Canvas 绘制，视觉需要继续按 Figma 轨道/滑块细节微调。
- 画面设置里的「码率（清晰度）」和「帧率（流畅度）」点击进入二级页。
- 二级页顶部导航固定，左侧是 24dp 热区内的 16dp 返回箭头，标题居中。
- 二级列表行高按 68dp 处理，左右内容在主容器 12dp 内边距中排列，底部分割线为 `rgba(234,247,255,0.05)`。
- 码率页包含 `自动 / 清晰 / 高清 / 超清 / 自定义`，自定义右侧为 `3 Mbps` 输入态视觉。
- 帧率页包含 `30帧 / 90帧 / 90帧 / 144帧`。
- 普通单选项点击后立即切换选中态，并刷新主页面对应显示值。
- 自定义码率点击后打开 Figma `2713:19434` 里的滚动选择器弹窗；取消不改变当前选择，完成后保存 Mbps 并选中「自定义」。
- 自定义弹窗遮罩为黑色 60%，弹窗宽 `240dp`，选择区高 `160dp`，底部按钮区高 `56dp`。

## Current Caveat

本机无法运行 Gradle，因为缺 Java Runtime：

```text
Unable to locate a Java Runtime.
```

因此目前只能做静态检查，未能在本机完成 `./gradlew assembleDebug`。
