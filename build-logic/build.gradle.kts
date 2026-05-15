// Root project of the build-logic composite build.
// Applying `base` registers the `clean` lifecycle task so that Android Studio's
// "Clean Project" action (which explicitly runs :build-logic:clean) succeeds.
// The actual convention plugin logic lives in :convention.
plugins {
    base
}
