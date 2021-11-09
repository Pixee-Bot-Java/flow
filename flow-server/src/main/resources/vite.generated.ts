/**
 * NOTICE: this is an auto-generated file
 *
 * This file has been generated by the `flow:prepare-frontend` maven goal.
 * This file will be overwritten on every run. Any custom changes should be made to vite.config.ts
 */
import path from 'path';
import * as net from 'net';

import { processThemeResources } from '@vaadin/application-theme-plugin/theme-handle.js';
import settings from '#settingsImport#';
import { UserConfigFn, defineConfig, HtmlTagDescriptor, mergeConfig } from 'vite';

const frontendFolder = path.resolve(__dirname, settings.frontendFolder);
const themeFolder = path.resolve(frontendFolder, settings.themeFolder);
const buildFolder = path.resolve(__dirname, settings.frontendBundleOutput);

const projectStaticAssetsFolders = [
  path.resolve(__dirname, 'src', 'main', 'resources', 'META-INF', 'resources'),
  path.resolve(__dirname, 'src', 'main', 'resources', 'static'),
  frontendFolder
];

// Folders in the project which can contain application themes
const themeProjectFolders = projectStaticAssetsFolders.map((folder) => path.resolve(folder, settings.themeFolder));

const themeOptions = {
  devMode: false,
  // The following matches folder 'target/flow-frontend/themes/'
  // (not 'frontend/themes') for theme in JAR that is copied there
  themeResourceFolder: path.resolve(__dirname, settings.themeResourceFolder),
  themeProjectFolders: themeProjectFolders,
  projectStaticAssetsOutputFolder: path.resolve(__dirname, settings.staticOutput),
  frontendGeneratedFolder: path.resolve(frontendFolder, settings.generatedFolder)
};

// Block debug and trace logs.
console.trace = () => {};
console.debug = () => {};

function updateTheme(contextPath: string) {
  const themePath = path.resolve(themeFolder);
  if (contextPath.startsWith(themePath)) {
    const changed = path.relative(themePath, contextPath);

    console.debug('Theme file changed', changed);

    if (changed.startsWith(settings.themeName)) {
      processThemeResources(themeOptions, console);
    }
  }
}

function runWatchDog(watchDogPort) {
  const client = net.Socket();
  client.setEncoding('utf8');
  client.on('error', function () {
    console.log('Watchdog connection error. Terminating vite process...');
    client.destroy();
    process.exit(0);
  });
  client.on('close', function () {
    client.destroy();
    runWatchDog(watchDogPort);
  });

  client.connect(watchDogPort, 'localhost');
}

export const vaadinConfig: UserConfigFn = (env) => {
  if (env.mode === 'development' && process.env.watchDogPort) {
    // Open a connection with the Java dev-mode handler in order to finish
    // vite when it exits or crashes.
    runWatchDog(process.env.watchDogPort);
  }
  return {
    root: 'frontend',
    base: env.mode === 'production' ? '' : '/VAADIN/',
    resolve: {
      alias: {
        themes: themeFolder,
        Frontend: frontendFolder
      }
    },
    build: {
      outDir: buildFolder,
      assetsDir: 'VAADIN/build',
      rollupOptions: {
        input: {
          indexhtml: path.resolve(frontendFolder, 'index.html')
        }
      }
    },
    plugins: [
      {
        name: 'custom-theme',
        config() {
          processThemeResources(themeOptions, console);
        },
        handleHotUpdate(context) {
          updateTheme(path.resolve(context.file));
        }
      },
      {
        name: 'inject-entrypoint-script',
        transformIndexHtml: {
          enforce: 'pre',
          transform(_html, context) {
            if (context.path !== '/index.html') {
              return;
            }
            const vaadinScript: HtmlTagDescriptor = {
              tag: 'script',
              attrs: { type: 'module', src: './generated/vaadin.ts' },
              injectTo: 'head'
            };
            return [vaadinScript];
          }
        }
      }
    ]
  };
};

export const overrideVaadinConfig = (customConfig: UserConfigFn) => {
  return defineConfig((env) => mergeConfig(vaadinConfig(env), customConfig(env)));
};
