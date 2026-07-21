# Legion 串流设置界面 · 交付规格说明

版本：v1.0  
日期：2026-07-21  
平台：原生 Android Java（minSdk 26，targetSdk 34）

---

## 一、整体容器

| 属性 | 值 |
|------|----|
| 宽度 | `300dp`，固定 |
| 高度 | 屏幕高度 − `16dp`（上下各留 `8dp`） |
| 位置 | 居右，距屏幕右边缘 `8dp`，垂直居中 |
| 圆角 | `12dp` |
| 背景色 | `rgb(27, 32, 39)` |
| 边框 | `0.5dp`，`rgba(255,255,255,0.24)` |
| 屏幕方向 | 强制横屏 |

---

## 二、动画

| 场景 | 动画 |
|------|------|
| 入场 | 从右侧滑入（translationX）+ 淡入，320ms，DecelerateInterpolator(2f) |
| 出场（关闭按钮） | 向右滑出 + 淡出，280ms，AccelerateInterpolator(2f) |
| 唤起（演示按钮） | 同入场动画 |

---

## 三、导航栏（顶部固定）

| 属性 | 值 |
|------|----|
| 高度 | `48dp` |
| 左侧 | 返回箭头（二级页）/ 空白（主页） |
| 标题 | 居中，`14sp`，`rgba(234,247,255,0.90)` |
| 右侧 | 关闭图标（主页）/ 空白（二级页） |
| 关闭热区 | `24dp`，图标 `16dp` |
| 返回热区 | `24dp`，图标 `16dp`，`ic_arrow_left` |
| 底部分割线 | `0.5dp`，`rgba(255,255,255,0.16)` |

---

## 四、内容区域滚动

- `StretchScrollView`：边界拉伸回弹，最大拉伸 `52dp`，阻尼 `0.42`，回弹 260ms
- `PanelScrollBar`：右侧自定义滚动条，滚动时淡入（160ms），停止 1.5s 后淡出（260ms）
- `FadeFooter`：底部渐隐遮罩，`32dp`，`rgb(27,32,39)` → 透明

---

## 五、主页模块

### 5.1 画面设置卡片

- 背景：`rgba(255,255,255,0.08)` 渐变，圆角 `2dp`
- 码率（清晰度）、帧率（流畅度）两行，点击进入二级页
- 底部说明文字：`12sp`，`rgba(234,247,255,0.40)`

### 5.2 透明度滑杆卡片（键盘 / 模拟手柄）

- 背景图：`overlay_keyboard_bg.png` / `overlay_gamepad_bg.png`
- 前景素材：`keyboard_material.png` / `gamepad_material.png`，尺寸 `276×160dp`
- 滑杆轨道高度 `6dp`，滑块 `28×18dp`，圆角 `4dp`
- 滑动实时更新透明度百分比和素材透明度

---

## 六、二级页面

### 6.1 码率（清晰度）

| 选项 | 说明 |
|------|------|
| 自动 | 根据网络状况智能调节 |
| 清晰 | 2 Mbps，适合弱网络 |
| 高清 | 8 Mbps，流畅清晰 |
| 超清 | 20 Mbps，极致画质 |
| 自定义 | 1–20 Mbps，点击打开选择器弹窗 |

### 6.2 帧率（流畅度）

| 选项 | 说明 |
|------|------|
| 30帧 | 省流省电，适合文字办公 |
| 60帧 | 均衡流畅，适合日常使用 |
| 90帧 | 高度流畅，适合演示 |
| 144帧 | 极致流畅，适合高性能设备 |

- 行高 `68dp`，左右内边距 `12dp`
- 分割线 `rgba(234,247,255,0.05)`
- 单选，点击立即切换并刷新主页显示值

---

## 七、自定义码率弹窗

| 属性 | 值 |
|------|----|
| 遮罩 | 黑色 60%，点击遮罩不关闭 |
| 弹窗尺寸 | `240×216dp` |
| 背景色 | `rgb(58,64,71)`，圆角 `12dp`，边框 `rgba(200,199,254,0.30)` |
| 选择区高度 | `160dp` |
| 底部按钮区高度 | `56dp` |
| 取消按钮 | `rgba(234,247,255,0.15)` 背景，圆角 `4dp` |
| 完成按钮 | `rgb(56,110,254)` 背景，圆角 `4dp` |
| 进场动画 | 从下往上 + 淡入，300ms，DecelerateInterpolator(2f) |
| 出场动画 | 向下 + 淡出，220ms，AccelerateInterpolator(2f) |

### 滚动选择器（BitratePicker）

- 范围：1–20 Mbps
- 每格高度：`36dp`
- 实时跟手，支持 fling 惯性滚动（VelocityTracker + OverScroller）
- 抬手后 snap 到最近整数，180ms 动画
- 每跨一格触发 `CLOCK_TICK` 触觉反馈
- 中心选中高亮：`rgba(0,0,0,0.24)`
- 上下渐隐遮罩与背景色一致：`rgba(58,64,71,0.82)`

---

## 八、资源文件清单

| 文件 | 说明 |
|------|------|
| `drawable/ic_close_large.xml` | 关闭图标，VectorDrawable，热区 `24dp`，图标 `16dp` |
| `drawable/ic_arrow_right.xml` | 右箭头，VectorDrawable，`16dp` |
| `drawable/ic_arrow_left.xml` | 左箭头，VectorDrawable，`16dp` |
| `drawable/ic_sort.xml` | 排序图标，VectorDrawable，`16dp` |
| `drawable-nodpi/keyboard_material.png` | 键盘前景素材，`276×160px` |
| `drawable-nodpi/gamepad_material.png` | 手柄前景素材，`276×160px` |
| `drawable-nodpi/overlay_keyboard_bg.png` | 键盘模块背景 |
| `drawable-nodpi/overlay_gamepad_bg.png` | 手柄模块背景 |

---

## 九、演示专用（交付时删除）

- `SettingsSurface.build()` 中的 `trigger` 按钮：屏幕左侧 `32×56dp`，点击唤起设置面板
- `drawable-nodpi/app_bg.png`：测试用背景图，交付时替换为实际游戏画面

---

## 十、核心文件

```
app/src/main/java/com/legion/streamsettings/MainActivity.java
```

所有 View 均为代码构建，无 XML layout，无第三方依赖。
