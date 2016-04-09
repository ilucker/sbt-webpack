sb-webpack
==========

sb-webpack is an [sbt-web] plugin for the [webpack module bundler].


Setup
----------

Add the following to your `project/plugins.sbt` file:

```scala
addSbtPlugin("com.ilucker" % "sbt-webpack" % "0.1.0")
```

Your project's build file also needs to enable sbt-web plugins. For example with build.sbt:

```scala
    lazy val root = (project.in file(".")).enablePlugins(SbtWeb)
```

Create a `package.json` file in the base directory and add webpack as a devDependancy to it:
```json
{
  "devDependencies": {
    "webpack": "^1.12.15"
  }
}
```

License
-------

Copyright 2016 Alexander Gavrilov.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0.html.


[sbt-web]: https://github.com/sbt/sbt-web
[webpack module bundler]: http://webpack.github.io/