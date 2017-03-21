# lein-test-diff

A leiningen plugin that lists tests that are affected by a code change.

# Usage

To install Ultra, just add the following to your `~/.lein/profiles.clj` or project.clj

```clojure
{:user {:plugins [lein-test-diff ["0.0.1-SNAPSHOT"]]}}
```

Run this to get list of files that are affected by code change

```clojure
git diff HEAD~ --name-only | lein test-diff

```

You can also pipe it to a test runner
```clojure
git diff HEAD~ --name-only | lein test-diff | lein test

```


## License

Copyright © 2017 Kapil Reddy

Distributed under the Eclipse Public License, the same as Clojure.
