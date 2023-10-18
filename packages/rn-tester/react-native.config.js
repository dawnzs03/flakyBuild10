/**
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @format
 */

'use strict';

// Inside the React Native monorepo, we need to explicitly extend the base
// CLI config as the adjacent package will not be conventionally discovered.
const config = require('../react-native/react-native.config.js');

module.exports = {
  ...config,
  reactNativePath: '../react-native',
  project: {
    ios: {
      sourceDir: '.',
      unstable_reactLegacyComponentNames: [
        'RNTMyLegacyNativeView',
        'RNTMyNativeView',
      ],
    },
    android: {
      sourceDir: '../../',
    },
  },
};
