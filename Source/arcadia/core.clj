(ns arcadia.core
  (:require
    clojure.string
    [arcadia.internal.map-utils :as mu])
  (:import 
    [Godot Node GD ResourceLoader 
      Node Node2D SceneTree Sprite Spatial]))

(defn log [& args]
  "Log message to the Godot Editor console. Arguments are combined into a string."
  (GD/Print (into-array (map #(str % " ") args))))

(defn ifn-arr [s]
  (let [xs  (re-seq #"[^ \t\n\r]+" s)
        fns
        (into-array clojure.lang.IFn
          (remove nil? 
            (map 
              (fn [s]
                (when-let [-ns (last (re-find #"#'([^/]+)\/.+" s))]
                  (try 
                    (require (symbol -ns))
                    (eval (read-string s))
                    (catch Exception e (log e)))))
              xs)))]
    fns))

(defn node-path [s] (Godot.NodePath. s))

(defn tree [node] (.GetTree node))

(defn group! 
  ([node s] (group! node s true))
  ([node s pers] (.AddToGroup node s pers)))

(defn load-scene [s]
  (let [scene (ResourceLoader/Load (str "res://" s) "PackedScene" true)]
    scene))


(defn get-node 
  "Uses the global scene viewport Node, \"/root/etc\""
  [s]
  (.GetNode (.Root (Godot.Engine/GetMainLoop)) (node-path s)))

(defn find-node 
  "Recursive find from global root, s is a wildcard string supporting * and ?"
  [s]
  (.FindNode (.Root (Godot.Engine/GetMainLoop)) s true false))

(defn instance [pscn]
  ;not so optional second arg, PackedScene.GenEditState.Disabled = 0
  (.Instance pscn 0))

(defn add-child [^Node node ^Node child]
  (.AddChild node child true))

(defn remove-child [^Node node ^Node child]
  (.RemoveChild node child))

(defn parent [^Node node]
  (.GetParent node (type-args Node)))

(defn children [^Node node]
  (.GetChildren node))

(defn destroy [^Node node]
  (.QueueFree node))

(defn destroy-immediate [^Node node]
  (.Free node))

(defn is-qeued-for-deletion? [^Node node]
  (.IsQueuedForDeletion node))


(def alphabet ["a" "b" "c" "d" "e" "f" "g" "h" "i" "j" "k" "l" "m" "n" "o" "p" "q" "r" "s" "t" "u" "v" "w" "x" "y" "z"])

(def variants-enum {
  nil             0
  System.Boolean  1
  System.Int32    2
  System.Single   3
  System.String   4
  Godot.Vector2   5
  Godot.Rect2     6
  Godot.Vector3   7
  Godot.Transform2D 8
  Godot.Plane     9
  Godot.Quat      10
  Godot.AABB      11
  Godot.Basis     12
  Godot.Transform 13
  Godot.Color     14
  Godot.NodePath  15
  Godot.RID       16
  Godot.Object    17
  Godot.Collections.Dictionary 18
  Godot.Collections.Array 19
  System.Byte     20
  |System.Int32[]|  21
  |System.Single[]| 22
  |System.String[]| 23
  |Godot.Vector2[]| 24
  |Godot.Vector3[]| 25
  |Godot.Color[]|   26 })

(defonce runtime-hook (RuntimeHook.))

;TODO this should work with vars and redefinable bindings
(defn connect [^Node node ^String signal-name f]
  "Connects a node's signal to a function.  These connections share a Godot.Object instance and only one connection can be made for each node's signal."
  (.Register runtime-hook (hash f) f)
  (.Connect node signal-name runtime-hook "CatchMethod" 
    (Godot.Collections.Array. (into-array Object [(hash f)])) 0))

(defn add-signal 
  "Adds a user signal to the object, which can then be emitted with `emit`. args should be type literals matching those listed in the Godot.Variant.Type enum"
  ([^Godot.Object o ^String name]
    (RuntimeHook/AddSignal o name))
  ([^Godot.Object o ^String name & args]
    (let [variants (remove nil? (map variants-enum args))
          names    (take (count variants) alphabet)]
      (RuntimeHook/AddSignal o name (into-array System.String names) (into-array System.Int32 variants)))))

(defn emit
  "Emit's a node's signal."
  ([^Godot.Object o ^String name]
    (.EmitSignal o name (|System.Object[]|. 0)))
  ([^Godot.Object o ^String name & args]
    (.EmitSignal o name (into-array Object args))))



'(let [n (Node.)] 
  (add-signal n "foo" Godot.Vector3)
  (connect n "foo" (fn [v] (log "FOO SIGNAL" v)))
  (emit (find-node "Button") "foo" (Godot.Vector3. 1 2 3)))