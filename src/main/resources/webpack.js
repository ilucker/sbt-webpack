/*global process, require */

(function () {"use strict";

    function formatError(e, severity) {
        if (typeof e === "string")
            e = {message: e};

        var problem = {
            message: e.message,
            severity: severity
        };

        if(e.dependencies && e.origin) {
            problem.source = e.origin.identifier();
            e.dependencies.slice(0, 1).forEach(function(dep) {
                if(!dep.loc || typeof dep.loc === "string" || !dep.loc.start) return;
                problem.lineNumber = dep.loc.start.line;
                problem.characterOffset = dep.loc.start.column
            });
        }

        return problem;
    }

    function mkConfig(config, options) {
        if (typeof config === "function") {
            return config(options);
        }
        return config;
    }

    var args = process.argv,
        path = require("path"),
        webpack = require("webpack");

    var configFile = args[2],
        options = JSON.parse(args[3]),
        config = require(configFile);

    var compiler = webpack(mkConfig(config, options));

    compiler.run(function (err, stats) {
        if (err) throw err;

        var compilation = stats.compilation;

        var problems = [];

        if (stats.hasErrors()) {
            compilation.errors.forEach(function (error) {
                problems.push(formatError(error, "error"))
            });
        }
        if (stats.hasWarnings()) {
            compilation.warnings.forEach(function (error) {
                problems.push(formatError(error, "warn"))
            });
        }

        var filesWritten = Object.keys(compilation.assets).map(function (asset) {
            return path.resolve(compiler.outputPath, asset);
        });
        var filesRead = compilation.modules.map(function (module) {
            return module.identifier();
        });
        process.stdout.write("\u0010" + JSON.stringify({filesWritten: filesWritten, filesRead: filesRead, problems: problems}) + "\n");
    });
})();