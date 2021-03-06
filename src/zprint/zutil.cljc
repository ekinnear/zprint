(ns zprint.zutil
  (:require clojure.string
            zprint.zfns
            [rewrite-clj.parser :as p]
            [rewrite-clj.node :as n]
            [rewrite-clj.zip :as z]
            #?@(:cljs [[rewrite-clj.zip.base :as zb]
                       [rewrite-clj.zip.whitespace :as zw] clojure.zip])))

;;
;; # Zipper oriented style printers
;;

;;
;; ## clj and cljs compatibility routines
;;
;; ### Routines missing in :cljs since it uses clojure.zip
;; 

(def down*
  #?(:clj z/down*
     :cljs clojure.zip/down))

(def up*
  #?(:clj z/up*
     :cljs clojure.zip/up))

(def right*
  #?(:clj z/right*
     :cljs clojure.zip/right))

(def left*
  #?(:clj z/left*
     :cljs clojure.zip/left))

(def next*
  #?(:clj z/next*
     :cljs clojure.zip/next))

(def prev*
  #?(:clj z/prev*
     :cljs clojure.zip/prev))

(def replace*
  #?(:clj z/replace*
     :cljs clojure.zip/replace))

;;
;; ### Routines with different namespaces
;;

(def edn*
  #?(:clj z/edn*
     :cljs zb/edn*))

(def sexpr
  #?(:clj z/sexpr
     :cljs zb/sexpr))

(def string
  #?(:clj z/string
     :cljs zb/string))

(def tag
  #?(:clj z/tag
     :cljs zb/tag))

(def skip
  #?(:clj z/skip
     :cljs zw/skip))

(def skip-whitespace
  #?(:clj z/skip-whitespace
     :cljs zw/skip-whitespace))

(def whitespace?
  #?(:clj z/whitespace?
     :cljs zw/whitespace?))

(def whitespace-or-comment?
  #?(:clj z/whitespace-or-comment?
     :cljs zw/whitespace-or-comment?))

;;
;; Check to see if we are at the focus by checking the
;; path.
;;

(declare find-root-and-path)

(defn zfocus
  "Is the zipper zloc equivalent to the path floc.  In this
  case, floc isn't a zipper, but was turned into a path early on."
  [zloc floc]
  (let [[_ zpath] (find-root-and-path zloc)] (= zpath floc)))

(defn zfocus-style
  "Take the various inputs and come up with a style."
  [style zloc floc]
  (let [style (if (= style :f) style (if (zfocus zloc floc) :f :b))] style))

(defn z-coll? "Is the zloc a collection?" [zloc] (z/seq? zloc))

(defn zuneval? "Is this a #_(...)" [zloc] (= (tag zloc) :uneval))

(defn zmeta? "Is this a ^{...}" [zloc] (= (tag zloc) :meta))

(defn zquote?
  "Is this a '(...) or '[ ... ] or some other quote?"
  [zloc]
  (= (tag zloc) :quote))

(defn zreader-macro? "Is this a @..." [zloc] (= (tag zloc) :reader-macro))

(defn ztag "Return the tag for this zloc" [zloc] (tag zloc))

(defn zparseuneval
  "Turn an uneval zloc with #_ starting it into a zipper."
  [zloc]
  (edn* (p/parse-string
          (clojure.string/triml
            (clojure.string/replace-first (string zloc) #"#_" "")))))

(defn zcreateuneval
  "Turn a zloc into an #_ uneval zipper."
  [zloc]
  (edn* (p/parse-string (clojure.string/triml (str "#_" (string zloc))))))

(defn zcomment?
  "Returns true if this is a comment."
  [zloc]
  (when zloc (= (tag zloc) :comment)))

(defn znewline?
  "Returns true if this is a newline."
  [zloc]
  (when zloc (= (tag zloc) :newline)))

(defn znumstr
  "Does z/string, but takes an additional argument for hex conversion.
  Hex conversion is not implemented for zippers, though, because at present
  it is only used for byte-arrays, which don't really show up here."
  [zloc _ _]
  (string zloc))

(defn zfirst
  "Find the first non-whitespace zloc inside of this zloc, or
  the first whitespace zloc that is the focus."
  [zloc]
  (let [nloc (down* zloc)] (if nloc (skip right* whitespace? nloc))))

(defn zsecond
  "Find the second non-whitespace zloc inside of this zloc."
  [zloc]
  (if-let [first-loc (zfirst zloc)]
    (if-let [nloc (right* first-loc)] (skip right* whitespace? nloc))))

(defn zrightnws
  "Find the next non-whitespace zloc inside of this zloc."
  [zloc]
  (if zloc (if-let [nloc (right* zloc)] (skip right* whitespace? nloc))))

(defn zrightmost
  "Find the rightmost non-whitespace zloc at this level"
  [zloc]
  (loop [nloc (zrightnws zloc)
         ploc zloc]
    (if-not nloc ploc (recur (zrightnws nloc) nloc))))

(defn zleftnws
  "Find the next non-whitespace zloc inside of this zloc."
  [zloc]
  (if zloc (if-let [nloc (left* zloc)] (skip left* whitespace? nloc))))

(defn zleftmost
  "Find the leftmost non-whitespace zloc at this level"
  [zloc]
  (loop [nloc (zleftnws zloc)
         ploc zloc]
    (if-not nloc ploc (recur (zleftnws nloc) nloc))))

(defn znextnws
  "Find the next non-whitespace zloc."
  [zloc]
  (if (z/end? zloc)
    zloc
    (if-let [nloc (next* zloc)] (skip next* whitespace? nloc))))

(defn zprevnws
  "Find the next non-whitespace zloc."
  [zloc]
  (if-let [ploc (prev* zloc)] (skip prev* whitespace? ploc)))

(defn znth
  "Find the nth non-whitespace zloc inside of this zloc."
  [zloc n]
  (loop [nloc (skip-whitespace (down* zloc))
         i n]
    (if (or (nil? nloc) (= i 0)) nloc (recur (zrightnws nloc) (dec i)))))

(defn zmap
  "Return a vector containing the return of applying a function to 
  every non-whitespace zloc inside of zloc."
  [zfn zloc]
  (loop [nloc (down* zloc)
         out []]
    (if-not nloc
      out
      (recur (right* nloc)
             (if-let [result (when (not (whitespace? nloc)) (zfn nloc))]
               (conj out result)
               out)))))

(defn zmap-all
  "Return a vector containing the return of applying a function to 
  every zloc inside of zloc."
  [zfn zloc]
  (loop [nloc (down* zloc)
         out []]
    (if-not nloc out (recur (right* nloc) (conj out (zfn nloc))))))

(defn zmap-right
  "Apply a function to every non-whitespace zloc to right of zloc."
  [zfn zloc]
  (loop [nloc (right* zloc)
         out []]
    (if-not nloc
      out
      (recur (right* nloc) (if (whitespace? nloc) out (conj out (zfn nloc)))))))

(defn zseqnws
  "Return a seq of all of the non-whitespace children of zloc."
  [zloc]
  (zmap identity zloc))

(defn zcount
  "How many non-whitespace children does zloc have?"
  [zloc]
  (count (zseqnws zloc)))

(defn find-root-and-path
  "Create a vector with the root as well as another vector
  which contains the number of right moves after each down
  down to find a particular zloc.  The right moves include
  both whitespace and comments."
  [zloc]
  (if zloc
    (loop [nloc zloc
           left 0
           out ()]
      (if-not (left* nloc)
        (if-not (up* nloc) [nloc out] (recur (up* nloc) 0 (cons left out)))
        (recur (left* nloc) (inc left) out)))))

(defn find-root-and-path-nw
  "Create a vector with the root as well as another vector
  which contains the number of right moves after each down
  down to find a particular zloc.  The right moves are
  non-whitespace, but include comments."
  [zloc]
  (if zloc
    (loop [nloc zloc
           left 0
           out ()]
      (if-not (left* nloc)
        (if-not (up* nloc) [nloc out] (recur (up* nloc) 0 (cons left out)))
        (recur (left* nloc) (if (whitespace? nloc) left (inc left)) out)))))

(defn find-root
  "Find the root from a zloc by doing lots of ups."
  [zloc]
  (loop [nloc zloc] (if-not (z/up nloc) nloc (recur (z/up nloc)))))

(defn move-down-and-right
  "Move one down and then right a certain number of steps."
  [zloc right-count]
  (loop [nloc (down* zloc)
         remaining-right right-count]
    (if (zero? remaining-right)
      nloc
      (recur (right* nloc) (dec remaining-right)))))

(defn follow-path
  "Follow the path vector from the root and return the zloc
  at this location."
  [path-vec zloc]
  (reduce move-down-and-right zloc path-vec))

(defn zanonfn? "Is this an anonymous fn?" [zloc] (= (tag zloc) :fn))

(defn zlast
  "Return the last non-whitespace (but possibly comment) element inside
  of this zloc."
  [zloc]
  (let [nloc (down* zloc)] (when nloc (zrightmost nloc))))

(defn zsexpr?
  "Returns true if this can be converted to an sexpr. Works around a bug
  where n/printable-only? returns false for n/tag :fn, but z/sexpr fails
  on something with n/tag :fn"
  [zloc]
  (and zloc (not= :fn (tag zloc)) (not (n/printable-only? (z/node zloc)))))

;
; This doesn't work, because there are situations where (zsexpr? zloc)
; will fail but it is still a keyword.
;
#_(defn zkeyword?-alt
    "Returns true if this is a keyword."
    [zloc]
    (and zloc (zsexpr? zloc) (keyword? (sexpr zloc))))

(defn zkeyword?
  "Returns true if this is a keyword."
  [zloc]
  (and zloc (clojure.string/starts-with? (z/string zloc) ":")))

(defn zsymbol?
  "Returns true if this is a symbol."
  [zloc]
  (and zloc (zsexpr? zloc) (symbol? (sexpr zloc))))

(defn znil?
  "Returns true if this is nil."
  [zloc]
  (and zloc (zsexpr? zloc) (nil? (z/sexpr zloc))))

(defn zreader-cond-w-symbol?
  "Returns true if this is a reader-conditional with a symbol in 
  the first position (could be :clj or :cljs, whatever)."
  [zloc]
  (let [result (when (zreader-macro? zloc)
                 (let [element (z/down zloc)]
                   (when (= (z/string element) "?")
                     (let [element (z/down (z/right element))]
                       (when (or (= (z/string element) ":clj")
                                 (= (z/string element) ":cljs"))
                         (zsymbol? (z/right element)))))))]
    #_(println "zreader-cond-w-symbol?:" (z/string zloc) "result:" result)
    result))

(defn zreader-cond-w-coll?
  "Returns true if this is a reader-conditional with a collection in 
  the first position (could be :clj or :cljs, whatever)."
  [zloc]
  (let [result (when (zreader-macro? zloc)
                 (let [element (z/down zloc)]
                   (when (= (z/string element) "?")
                     (let [element (z/down (z/right element))]
                       (when (or (= (z/string element) ":clj")
                                 (= (z/string element) ":cljs"))
                         (z-coll? (z/right element)))))))]
    #_(println "zreader-cond-w-coll?:" (z/string zloc) "result:" result)
    result))

(defn zdotdotdot
  "Return a zloc that will turn into a string of three dots."
  []
  (edn* (p/parse-string "...")))

(defn zconstant?
  "Returns true if this is a keyword, string, or number, in other words,
  a constant."
  [zloc]
  #_(println "zconstant?" (z/string zloc))
  (let [ztag (z/tag zloc)]
    (if (or (= ztag :unquote) (= ztag :quote) (= ztag :syntax-quote))
      (zconstant? (zfirst zloc))
      (and (not (z-coll? zloc))
           (or (zkeyword? zloc)
               #_(println "zconstant? - not keyword:" (z/string zloc))
               (when (zsexpr? zloc)
                 #_(println "zconstant?:" (z/string zloc)
                            "\n z-coll?" (z-coll? zloc)
                            "z/tag:" (z/tag zloc))
                 (let [sexpr (sexpr zloc)]
                   (or (string? sexpr) (number? sexpr)))))))))

;;
;; # Integrate specs with doc-string
;;
;; Find find-docstring could be a lot smarter, and perhaps
;; find the docstring in the meta data (so that, defn might
;; work, for instance).

(defn find-docstring
  "Find a docstring in a zipper of a function."
  [zloc]
  (let [fn-name (z/string (z/down zloc))]
    (cond (or (= fn-name "defn") (= fn-name "defmacro"))
            (let [docloc (z/right (z/right (z/down zloc)))]
              (when (string? (sexpr docloc)) docloc))
          :else nil)))

(defn add-spec-to-docstring
  "Given a zipper of a function definition, add the spec info
  to the docstring."
  [zloc spec-str]
  #_(println "spec-str:" spec-str)
  (if-let [doc-zloc (find-docstring zloc)]
    (let [new-doc-zloc (replace* doc-zloc
                                 (z/node (edn* (p/parse-string
                                                 (str "\""
                                                      (str (sexpr doc-zloc))
                                                      spec-str
                                                      "\"")))))]
      (edn* (z/root new-doc-zloc)))
    zloc))

(defn zredef-call
  "Redefine all of the traversal functions for zippers, then
  call the function of no arguments passed in."
  [body-fn]
  (with-redefs [zprint.zfns/zstring z/string
                zprint.zfns/znumstr znumstr
                zprint.zfns/zbyte-array? (constantly false)
                zprint.zfns/zcomment? zcomment?
                zprint.zfns/zsexpr sexpr
                zprint.zfns/zseqnws zseqnws
                zprint.zfns/zmap-right zmap-right
                zprint.zfns/zfocus-style zfocus-style
                zprint.zfns/zfirst zfirst
                zprint.zfns/zsecond zsecond
                zprint.zfns/znth znth
                zprint.zfns/zcount zcount
                zprint.zfns/zmap zmap
                zprint.zfns/zanonfn? zanonfn?
                zprint.zfns/zfn-obj? (constantly false)
                zprint.zfns/zfocus zfocus
                zprint.zfns/zfind-path find-root-and-path
                zprint.zfns/zwhitespace? whitespace?
                zprint.zfns/zlist? z/list?
                zprint.zfns/zvector? z/vector?
                zprint.zfns/zmap? z/map?
                zprint.zfns/zset? z/set?
                zprint.zfns/zcoll? z-coll?
                zprint.zfns/zuneval? zuneval?
                zprint.zfns/zmeta? zmeta?
                zprint.zfns/ztag ztag
                zprint.zfns/zparseuneval zparseuneval
                zprint.zfns/zlast zlast
                zprint.zfns/zarray? (constantly false)
                zprint.zfns/zatom? (constantly false)
                zprint.zfns/zderef (constantly false)
                zprint.zfns/zrecord? (constantly false)
                zprint.zfns/zns? (constantly false)
                zprint.zfns/zobj-to-vec (constantly nil)
                zprint.zfns/zexpandarray (constantly nil)
                zprint.zfns/znewline? znewline?
                zprint.zfns/zwhitespaceorcomment? whitespace-or-comment?
                zprint.zfns/zmap-all zmap-all
                zprint.zfns/zpromise? (constantly false)
                zprint.zfns/zfuture? (constantly false)
                zprint.zfns/zdelay? (constantly false)
                zprint.zfns/zkeyword? zkeyword?
                zprint.zfns/zconstant? zconstant?
                zprint.zfns/zagent? (constantly false)
                zprint.zfns/zreader-macro? zreader-macro?
                zprint.zfns/zarray-to-shift-seq (constantly nil)
                zprint.zfns/zdotdotdot zdotdotdot
                zprint.zfns/zsymbol? zsymbol?
                zprint.zfns/znil? znil?
                zprint.zfns/zreader-cond-w-symbol? zreader-cond-w-symbol?
                zprint.zfns/zreader-cond-w-coll? zreader-cond-w-coll?]
    (body-fn)))