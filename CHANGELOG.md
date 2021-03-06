# Change Log
All notable changes to this project will be documented in this file. 

## 0.3.2 - 2017-4-18

### Changed

### Fixed

* Issue #23, where (czprint-fn defn {:style :justified}) would not
  produce any output.  This bug was added in 0.3.0.

* More work on dependencies to make it easier to release.

## 0.3.1 - 2017-4-10

### Fixed

* Fixed dependencies so that `[clojure-future-spec "1.9.0-alpha15"]`
  is actually required to use zprint on Clojure 1.8, and doesn't 
  get brought along into usage on 1.9 by default.

## 0.3.0 - 2017-4-9

### Changed

* __DEPRECATED__ configuration from environment variables and Java system
  properties.  Still available by adding a library to your dependencies.
  File an issue if you care about this!

* __DEPRECATED__ :auto-width capability.  Still available by adding a library to your dependencies.  File an issue if you care about this!

* Added zprint-filter uberjar and documentation.  Uberjar is available as
  a "release" on Github.

* Moved to clojure.spec for all versions, requiring a single additional
  library when using Clojure 1.8.

* Added futures to fzprint-hang-remaining, helped a lot.

* Used pmap to handle two-up generation, helped a lot.

* Added transient to some vectors being built.  Sometimes it helped, somtimes
  it didn't.  Kept the ones where it helped. 

### Fixed

* Dates in this file.

## 0.2.16 - 2017-2-27

### Changed

* Added zprint.lumo namespace to support zprint filter.  Only used when
  initiated by lumo.

* Added zprint.planck namespace to support zprint filter.  Only used when
  initiated by plank.
  
* Replaced function-by-function destructuring with with-redefs in both
  zutil.cljc and sutil.cljs.  Added new namespace zfns to make this work.
  Resulted in small but significant speedup.

* Major changes to Clojurescript port -- removed Prismatic/Plumatic Schema
  and replaced it with spec for checking options maps.  Refactored option
  map validation into two additional namespaces.  Now zprint will work
  in self-hosted Clojurescript, and we are ready for a complete move to
  spec when we can require 1.9.

## 0.2.15 - 2017-1-24

### Changed

* Added `{:map {:key-color {:key1 :color1 :key2 :color2 ...}}}` which will color the specified
  keys with the specified color.

* Added `{:map {:key-depth-color [:level-1-color :level-2-color ...]}}`, which
  will color the map keys based on depth, not their type.  The
  `:key-color {:key :color ...}` map will override any values from the 
  `:key-depth-color` map.  EXPERIMENTAL feature, might go away.

## 0.2.14 - 2017-1-22

### Changed

* Added a new capability to `:extend`, `:modfiers #{"static"}`, which
  will allow "static" to appear on the same line as protocol or type
  when formatting extends.  You remove elements from the set  with 
  `{:remove {:extend {:modifiers #{"static"}}}}` just like the other 
  sets are changed.  Issue #10.

* Added `defui` to the `:fn-map` as `:arg1-extend` so that when formatting
  Clojurescript om/next code you get the `static` elements to format 
  correctly.  Issue #10.

* Added several new styles: `:map-nl`, `:binding-nl`, `:pair-nl`.  Also
  added substantially to the documentation of styles in the readme.

* Added the first tests for the `:explain` output, at least the `:value`
  part of it.

### Fixed

* A problem when adding an element to any set in the options map, where the
  element was added, but the :explain output was incorrect.  Issue #21.

## 0.2.13 - 2017-1-19

### Changed

* Added support for `:hang?` in `:extend`, so that you can control
  whether or not the function definitions end up formatting as a
  hang after the type.  `:hang?` defaults to `true` (and this is
  not a change), but can now be turned off to force a flow.  Note
  that if a type and function defnition all fit on one line, that
  `:hang? false` will not affect it.  If you want them on separate
  lines you need to use `:flow? true`, which will override the normal
  desire to print things on one line.

* Altered the meaning of :nl-separator?.  In its initial release it
  would place an extra new-line whenever a pair took more than one line.
  Now, it only places an extra new-line when the pair did an actual
  flow, so that a multi-line hang does not trigger an extra new-line.
  The previous capability would be easy to add if there is any
  interest in doing that.

* Added the ability to set multiple styles at once, with a sequence
  of style keywords instead of just a single keyword.  Also added
  some great style tests.  Go expectations!

### Fixed

* The new `:nl-separator?` capability does not work right with lists
  that have constant-pairs at the end but regular s-expressions earlier.
  If the earlier s-expressions take more than one line, then they get
  blank lines below them which is wrong from several different perspectives.
  Now multiple line s-expressions don't ever trigger blank lines, since
  the `:nl-separator?` blank lines are triggered by a pair with the right
  hand part of the pair being formatted with a flow. Issue #17.

## 0.2.12 - 2017-1-9

### Fixed

* Accepted pull request to change function style for "ns" to `:arg1-body`.
  Thanks to pesterhazy!

* Accepted pull request to change function "!=" to "not=".  Thanks to
  mynomoto!

## 0.2.11 - 2017-1-8

### Changed

* Removed `:indent n` from `:pair-fn`, as it wasn't used.

* Changed `cond` and `alt` to be `:pair-fn`, and not `:pair`, since
  `:pair` was supposed to not be a function style on its own, but a
  configuration element for functions that have pairs in them, like
  `:arg1-pair` and `:pair-fn`.

* Added capability to produce blank lines after pairs where the second element
  of the pair doesn't fit on the same line as the first.  The options key
  for this is `:nl-separator?`, and is by default false for all occurances
  of this configuration option.  It is supported for `:map`, `:extend`,
  `:binding`, and `:pair` formatting.  This was requested for `:map`, but
  the implementation also allows configuring `:extend` to produce a commonly
  used format as well. Issue #12.

* Added `:flow?` options key for `:map`, `:extend`, `:binding`, and `:pair`.
  By default false for all.  When enabled, this will cause the second
  element of every pair to be formatted on the line after the first element,
  thus acting line a "flow".  See the readme for differences between
  `:flow?`, `:hang?`, `:force-nl?`, and `:nl-separator?`.  This was
  largely added to work together with `:nl-separator?` in `:extend`, but
  also has uses elsewhere.  `:flow?` will override the indent checks in
  when printing things two up, so that it will force a flow even though
  the first element of the pair is so short that it doesn't make any
  sense.  Just because it is so confusing if it doesn't do that.

### Fixed

* Fixed constant pairs to be formatted under control of the configuration
  for `:pair`, which now includes `:flow?` and `:nl-separator?`.

* Serious bug where whitespace after left parenthesis causes either the 
  first or second element in a list to be repeated if the list doesn't fit
  entirely on one line.  Issue #16.


## 0.2.10 - 2016-12-23

### Changed

* Added two new sets in the config, :fn-gt2-force-nl and :fn-gt3-force-nl
  into which you can put fn-styles so that they will never print on the
  same line even if they could.  The gt2 is if it has greater than 2 args
  and the gt3 is likewise if it has greater than 3 args.  Issue #3.

* Refactored `zprint-file`, pulling out `zprint-file-str` so that boot-fmt
  can use it.  Added `zprint-file-str` to the API.  Issue #7.

* Added `:arg2-fn` to support proxy.

* Added `:arg1-force-nl` to support `s/def`.  Put several additional
  spec functions into the `:fn-map` so that spec files now pretty nicely.

* Added experimental options to try to speed up map printing at the
  repl, which slows down when the maps are really deeply nested.
  `:max-hang-span`
  `:max-hang-depth`
  `:max-hang-count`
  These are experimental, and will probably not keep their current names
  or location in the options map.  They are not documented.  Do not 
  depend on them.

* Added `:flow` and `:flow-body` to force some functions to never hang.
  Largely added for `:require` in `ns` declarations.  Issue #5.

* Added `:require` to the `:fn-map` as `:force-nl-body` so that it will
  hang all of the requires.  If you want it to pull them 
  all over to the left and not even have something on the same line as
  the `:require`, define it as `:flow-body`.  Issue #2.

### Fixed

* Added a capability to remove elements from a set, now that sets in
  the config are more common.   Also, fixed it so that you can specify
  additional members for a set, and they will be merged into the set,
  instead of replacing the entire set.  Issue #4.

* Fixed `:fn-force-nl` to accurately match the function type, and not
  do it after the `-body` has been removed.  Issue #6.

* A bug in `:arg1-extend` where, when used for `proxy`, some of the function
  was getting dropped and not output.  Since `proxy` was not defined as
  `:arg1-extend`, this is not as serious as it might seem, but it was not
  good.

## 0.2.9 - 2016-11-13

### Changed

* Added :force-nl and :force-nl-body along with :fn-force-nl set in
  configuration to force some functions to never format onto the same
  line.  Primarily for -> and ->> and other similar functions where
  most people want to see them listed vertically, not on the same line.

* Made keywords format as functions, since they are.  This cleans up
  ns declarations a bit at the start of files.
  
### Fixed

## 0.2.8 - 2016-11-9

### Changed

* Added documentation on experimental features: auto-width, max-length,
  max-depth.

### Fixed

* Fixed bug in handling of :left-space :keep and :drop which involved a 
  complete rework of options map handling in process-multiple-forms
  and zprint-str-internal and czprint-str-internal.  Should have fixed
  a number of other not yet encountered bugs.
* Fixed issue in fzprint-hang-remaining where if the hang-expand value was
  exceeded it wouldn't bother to hang.  Which interacted badly with the
  hang-if-equal-flow? capability.  Added test to ensure this stays fixed.
* Fixed bug where if the first element of a pair was a map that got a 
  comma, and the second element of the pair ended up with a hang, the 
  first line of the hang would have the wrong indent because the size of
  the first element of the pair was calculated incorrectly.
* Fixed a bug where :map {:comma? false} didn't actually work if the map
  fit on one line.  Worked fine for multi-line maps, not one line ones.

## 0.2.7 - 2016-11-1

### Changed

* Changed hang-expand to 4.0 most everywhere
* Changed fn-map of -> to :none-body, and catch to :arg2
* Considerable changes to support Clojurescript.  All files now .cljc
* Added {:file {:left-space :keep}} or :drop to handle spaces on the
  left of function definitions.
* {:parse-string? true} will always trim spaces off of what is being parsed.
* (:parse-string-all? true} will parse multiple forms in one string.  This
  does not support ;!zprint {<options-map>} comments.

### Fixed

* Fixed bug where -> affected :arg1 status of more than immediate args
* Fixed issue #1 with incorrect indent on 3rd arg to (fn ...)

## 0.2.6 - 2016-10-10

### Changed

* Changes to support :reader-cond {:force-nl? boolean}
* Added defrecord to function categorization.

## 0.2.5 - 2016-10-06

### Changed

* Changed heuristics so that if hang and flow take the same number of
lines, then it will hang.  Can be adjusted with tuning.

## 0.2.4 - 2016-10-05

### Changed

* Reworked README.md, internal TOC, added information about several
existing capabilities.  Alphabetized the configurable elements.

## 0.2.3 - 2016-10-01

### Changed

* Removed `:list {:indent-body n}`, made it `:indent`.  Made `:indent-arg`
optional, uses `:indent` if it doesn't appear.
* Added `:support` keyword (replaced undocumented `:explain-all`).
* Added documentation for `:justify?` in readme.
* Added `:key-ignore` and `:key-ignore-silent` for `:map` formatting.
* Added `:to-string?` for `:record` formatting.

### Removed

* Removed (get-options) from zprint.core, took need for it out of readme.

### Fixed

* A problem where the last newline in a file was getting lost.
* A problem where a comment inside of a fn starting in col 0 was getting
an extra newline.

## 0.2.2 - 2016-09-24
### Changed

* Straighten out core and config namespaces for API
* Added :old? and :format to options map for lein-zprint

## 0.2.1 - 2016-09-22
### Changed

* Moved some configuration functions into zprint.core to match readme.
* Put ALPHA banner in readme.

* Fixed a bug where it wouldn't configure if no ~/.zprintrc file.

## 0.2.0 - 2016-09-21
### Added
- Initial project commit.

