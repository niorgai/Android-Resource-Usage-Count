# Android-Resource-Usage-Count
Android Resource Usage Count Plugin 

**DEPRECATED**

Due to IDE's find usage action cause too much memory, use this plugin will block your pc maybe after AS 3.0.

If I find some solution, i will reopen this repo.

[中文版](http://niorgai.github.io/2017/08/01/Android-Resource-Usage-Count/)

Auto count resource usage and show it in the left of each line.

Use in Android Studio and IntelliJ IDEA.

If count not show, please edit/reopen it.

[Jetbrains Plugin Page](https://plugins.jetbrains.com/plugin/9885-android-resource-usage-count)

![](http://7sbqys.com1.z0.glb.clouddn.com/resouce_count_plugin_example.jpeg)

Tag to count
---

* `array`
* `attr`
* `bool`
* `color`
* `declare-styleable`
* `dimen`
* `drawable`
* `eat-comment`
* `fraction`
* `integer`
* `integer-array`
* `item`
* `plurals`
* `string`
* `string-array`
* `style`

Result Color
---
* 0 - grey color
* 1 - blue color
* other - red color

Custom color in `Preferences` - `Other Settings` - `Android Resource Usage Count`

Path not count
---
* build/
* bin/