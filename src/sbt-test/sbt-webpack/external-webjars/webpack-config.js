'use strict';

var path = require('path');

module.exports = function (options) {
    return {
        context: options.sourceDirectory,
        entry: "entry.js",
        resolve: {
            root: options.rootDirectories
        },

        target: 'web',
        output: {
            path: options.targetDirectory,
            publicPath: '',
            filename: "bundle.js",
            pathInfo: true
        }
    }
};
