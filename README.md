# 地图画编辑器
## 简介
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;本软件是一款安卓手机软件，作者是我的世界魔灵工作室的创造者MC（即本仓库的创建者）。软件旨在方便的帮助玩家在手机基岩版上生成地图画。软件支持自动读取国际版（包括测试版，因为测试版和正式版不共存就不单独提了）、网易版、网易测试版的地图。

## 什么是地图画
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;本软件及其文档所指的地图画，均为游戏Minecraft（我的世界）中的“地图”物品上显示的图片。并非指在世界中生成方块。

## 使用方法

###  安卓10及其以下

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;第一次打开软件时，软件会向用户申请必要的存储权限。同意后，软件会自动读取您设备中的游戏内的世界存档。
<div align=center><img src="https://s2.loli.net/2022/04/18/aGn4uty8mzMRJTh.jpg"/>
	<br>
  <div style="color:orange; border-bottom: 1px solid #d9d9d9;
    display: inline-block;
    color: #999;
    padding: 2px;">
    主界面
  </div>
</div>
<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;点击一个你想生成地图画的世界。然后来到另一界面，提示你选择一种生成地图画的模式。

 - 普通模式：生成单张地图物品
 - 网格切分模式：按照用户指定的行列规则切割图片，生成多张地图物品
<div align=center><img src="https://s2.loli.net/2022/04/18/cVS2osEFPth1m4Y.jpg"/>
	<br>
  <div style="color:orange; border-bottom: 1px solid #d9d9d9;
    display: inline-block;
    color: #999;
    padding: 2px;">
    模式选择
  </div>
</div>
<br>
<div align=center><img src="https://s2.loli.net/2022/04/18/KW51zIw6GO2cTeu.jpg"/>
	<br>
  <div style="color:orange; border-bottom: 1px solid #d9d9d9;
    display: inline-block;
    color: #999;
    padding: 2px;">
    普通模式
  </div>
</div>
<br>
<div align=center><img src="https://s2.loli.net/2022/04/18/eNk79xcw4iqHUzr.jpg"/>
	<br>
  <div style="color:orange; border-bottom: 1px solid #d9d9d9;
    display: inline-block;
    color: #999;
    padding: 2px;">
    网格切分
  </div>
</div>
<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;具体每种模式的使用方法在软件内都有，这里不再赘述。

### 安卓11及其以上
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;因安卓11对Android/data目录中的文件进行了严格的权限控制，所以使用该软件的方式与安卓10及其以下的设备稍有不同，并略微麻烦一点。

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;第一次打开软件时，点击右上角的三个点，选择“导入游戏内地图”。然后软件会向您申请Android/data目录下的访问权限（这是必要的）。取得权限后会自动读取游戏内的地图，并出现一个新界面（这个过程可能等待时间略长）。然后在新界面中选择一个世界，软件就会把该世界复制到软件内的存储目录下。接下来，用户即可像安卓10及其以下的设备那样正常操作。

> 注意：生成地图物品结束后，因为安卓11的权限问题，您必须使用像MT文件管理器，ZArchiver等能够操作Android/data目录下的文件的文件管理器，将复制到软件内的世界再手动导入回游戏的存储目录中（游戏存储位置请自行搜索）。

<div align=center><img src="https://s2.loli.net/2022/04/18/bv3YUtuZ8DqMehf.png"/>
	<br>
  <div style="color:orange; border-bottom: 1px solid #d9d9d9;
    display: inline-block;
    color: #999;
    padding: 2px;">
    导入地图
  </div>
</div>
<br>
<div align=center><img src="https://s2.loli.net/2022/04/18/E9lOnISqyj4dbtM.png"/>
	<br>
  <div style="color:orange; border-bottom: 1px solid #d9d9d9;
    display: inline-block;
    color: #999;
    padding: 2px;">
    导入后
  </div>
</div>

## 最终效果

<div align=center><img src="https://s2.loli.net/2022/04/18/MKWkbrTVfXjsucD.jpg"/>
	<br>
  <div style="color:orange; border-bottom: 1px solid #d9d9d9;
    display: inline-block;
    color: #999;
    padding: 2px;">
    效果展示
  </div>
</div>

## 依赖库

 - [android-leveldb](https://github.com/mithrilmania/android-leveldb)
 - [leveldb-mcpe](https://github.com/Mojang/leveldb-mcpe)
 - [blocktopograph](https://github.com/oO0oO0oO0o0o00/blocktopograph)
