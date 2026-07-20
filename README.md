# Legion 串流设置面板

这是一个可直接用 Android Studio 打开的原生 Android 工程，当前实现横屏串流场景中的设置主框架。

## 当前实现

- Phone 面板宽度：`300dp`
- Pad 面板宽度：`400dp`
- 右侧边距：`8dp`
- 画面设置、键盘透明度、模拟手柄透明度
- 码率和帧率二级页面替换主页面
- 关闭按钮隐藏整个设置面板
- 强制横屏，不支持屏幕旋转

## 打开方式

在 Android Studio 中选择 `Open`，打开当前文件夹即可。第一次同步时 Android Studio 会自动配置本机 SDK 和 Gradle。

预览图目前使用内置绘制占位，后续将 3 倍 PNG 放入 `app/src/main/res/drawable-nodpi/`，替换 `previewKeyboard` 和 `previewPad` 即可，不需要修改页面尺寸逻辑。
