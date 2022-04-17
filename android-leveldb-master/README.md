# leveldb-android

[leveldb](https://github.com/google/leveldb) for Android.

Very simple bindings for using Leveldb from android.
There is an ~~excellent~~ outdated project [leveldbjni](https://github.com/fusesource/leveldbjni)
 out there for using leveldb from Java but seems to be a bit too much.
It has a bunch of dependencies, including a code generator,
 which might or might not work on Android. At least it is not trivial to get started.


## Dependencies

The only external dependencies are Android SDK and NDK.


## Building.

1. Clone the project with git. (Including the leveldb git-submodule, change it to a different fork if you want)
2. Open it with Android Studio. (Easiest way, but building could also be done with just gradle)
3. Build the project with gradle (if it was not already started automatically)
4. Make the project (Build > Make project)
5. Library should be ready now!


## Installation

After building, include the library module in your own project like this:

`your-project/settings.gradle`

    include ':android-leveldb'
    project(':android-leveldb').projectDir = new File(rootProject.projectDir, '../android-leveldb/library')

This assumes that you cloned the android-leveldb git repository in the parent-folder of your-project.

And add the module as dependency in `your-project/app/build.gradle`:

    //include .so files in the build (native libraries)
    compile fileTree(include: ['*.jar', '*.so'], dir: 'libs')

    //include the android-leveldb library (see settings.gradle)
    compile project(':android-leveldb')



## License

Please see the [LICENSE](LICENSE) file.


## Contributing

Contributions are always welcome.

See [CONTRIBUTORS.md](CONTRIBUTORS.md) for a list of contributors, forks, etc.

### TODO

Contributor guidelines are not set up yet.
