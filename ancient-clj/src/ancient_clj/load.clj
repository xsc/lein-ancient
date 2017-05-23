(ns ancient-clj.load)

(defn load-versions!
  "Load all available versions for the given artifact. `loaders`
   should be a map associating a repository ID with a two-parameter
   function producing a seq of maps with `:version`/`:version-string`
   keys based on artifact group and ID.

   Result will be a map associating each repository ID either with the
   respective seq or an instance of Throwable."
  [loaders {:keys [group id]}]
  {:pre [(map? loaders)
         (every? fn? (vals loaders))]
   :post [(map? %)
          (every?
            (fn [x]
              (or (coll? x) (instance? Throwable x)))
            (vals %))]}
  (->> loaders
       (pmap
         (fn [[loader-id f]]
           (try
             [loader-id (f group id)]
             (catch Throwable ex
               [loader-id ex]))))
       (into {})))
