# CustomBottomSheetBehavior
CustomBottomSheetBehavior Library for Android
  - Create Anchor Bottom Sheet
  - Fixed Tab Layout Scrolling on View Pager
  
### Example
You can import the example of this Library, or you can see this video :

[![Watch the video](https://media.giphy.com/media/xUOwG8ad0vXMl3zIOY/giphy.gif)](https://youtu.be/oD3xuXJ8wQ0)

### Installation

Add the maven repo url to your build.gradle on projek:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```

Add the library to the dependencies:

```groovy
dependencies {
    implementation 'com.github.rahmadarifan:CustomBottomSheetBehavior:v0.0.1'
}
```

#### Create Anchor Bottom Sheet
Use `CustomBottomSheetBehavior` and set app:behavior for your bottom sheet view:
```
app:behavior_anchorOffset="224dp"
app:behavior_defaultState="anchored"
app:behavior_peekHeight="192dp"
app:layout_behavior="com.rahmadarifan.library.custombottomsheetbehavior.CustomBottomSheetBehavior"
```
#### Fixed Tab Layout Scrolling on View Pager
Setup your `ViewPager` inside the bottom sheet with BottomSheetUtils:
```
BottomSheetUtils.setupViewPager(viewPager);
```