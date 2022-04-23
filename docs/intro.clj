;; # Tablecloth

^{:kind/hidden true}
(ns intro
  (:require [tablecloth.api :as tc]
            [scicloj.clay.v1.api :as clay]
            [scicloj.clay.v1.tools :as tools]
            [scicloj.clay.v1.tool.scittle :as scittle]
            [scicloj.kindly.v2.kind :as kind]
            [scicloj.clay.v1.view.dataset]))

^{:kind/hidden true}
(clay/start! {:tools [tools/scittle]})

^{:kind/hidden true}
(comment
  (do (scittle/show-doc! "docs/intro.clj")
      (scittle/write-html! "docs/intro.html")))

^{:kind/hidden true}
(def tech-ml-version (get-in (read-string (slurp "deps.edn")) [:deps 'techascent/tech.ml.dataset :mvn/version]))

tech-ml-version

;;
;; ## Introduction
;;
;; [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset) is a great and fast library which brings columnar dataset to the Clojure. Chris Nuernberger has been working on this library for last year as a part of bigger `tech.ml` stack.
;;
;; I've started to test the library and help to fix uncovered bugs. My main goal was to compare functionalities with the other standards from other platforms. I focused on R solutions: [dplyr](https://dplyr.tidyverse.org/), [tidyr](https://tidyr.tidyverse.org/) and [data.table](https://rdatatable.gitlab.io/data.table/).
;;
;; During conversions of the examples I've come up how to reorganized existing `tech.ml.dataset` functions into simple to use API. The main goals were:
;;
;; * Focus on dataset manipulation functionality, leaving other parts of `tech.ml` like pipelines, datatypes, readers, ML, etc.
;; * Single entry point for common operations - one function dispatching on given arguments.
;; * `group-by` results with special kind of dataset - a dataset containing subsets created after grouping as a column.
;; * Most operations recognize regular dataset and grouped dataset and process data accordingly.
;; * One function form to enable thread-first on dataset.
;;
;; If you want to know more about `tech.ml.dataset` and `dtype-next` please refer their documentation:
;;
;; * [tech.ml.dataset walkthrough](https://techascent.github.io/tech.ml.dataset/walkthrough.html)
;; * [dtype-next overview](https://cnuernber.github.io/dtype-next/overview.html)
;; * [dtype-next cheatsheet](https://cnuernber.github.io/dtype-next/cheatsheet.html)
;;
;; [SOURCE CODE](https://github.com/scicloj/tablecloth)
;;
;; Join the discussion on [Zulip](https://clojurians.zulipchat.com/#narrow/stream/236259-tech.2Eml.2Edataset.2Edev/topic/api)
;;
;; Let's require main namespace and define dataset used in most examples:
;;


(require '[tablecloth.api :as tc]
         '[tech.v3.datatype.functional :as dfn])
(def DS (tc/dataset {:V1 (take 9 (cycle [1 2]))
                     :V2 (range 1 10)
                     :V3 (take 9 (cycle [0.5 1.0 1.5]))
                     :V4 (take 9 (cycle ["A" "B" "C"]))}))


;;


DS


;;
;; ## Functionality
;;
;; ### Dataset
;;
;; Dataset is a special type which can be considered as a map of columns implemented around `tech.ml.datatype` library. Each column can be considered as named sequence of typed data. Supported types include integers, floats, string, boolean, date/time, objects etc.
;;
;; #### Dataset creation
;;
;; Dataset can be created from various of types of Clojure structures and files:
;;
;; * single values
;; * sequence of maps
;; * map of sequences or values
;; * sequence of columns (taken from other dataset or created manually)
;; * sequence of pairs: `[string column-data]` or `[keyword column-data]`
;; * array of native arrays
;; * file types: raw/gzipped csv/tsv, json, xls(x) taken from local file system or URL
;; * input stream
;;
;; `tc/dataset` accepts:
;;
;; * data
;; * options (see documentation of `tech.ml.dataset/->dataset` function for full list):
;;     - `:dataset-name` - name of the dataset
;;     - `:num-rows` - number of rows to read from file
;;     - `:header-row?` - indication if first row in file is a header
;;     - `:key-fn` - function applied to column names (eg. `keyword`, to convert column names to keywords)
;;     - `:separator` - column separator
;;     - `:single-value-column-name` - name of the column when single value is provided
;;     - `:column-names` - in case you want to name columns - only works for sequential input (arrays)
;;     - `:layout` - for numerical, native array of arrays - treat entries `:as-rows` or `:as-columns` (default)
;;
;; `tc/let-dataset` accepts bindings `symbol`-`column-data` to simulate R's `tibble` function. Each binding is converted into a column. You can refer previous columns to in further bindings (as in `let`).
;;
;; ---
;;
;; Empty dataset.
;;


(tc/dataset)


;;
;; ---
;;
;; Dataset from single value.
;;


;; (tc/dataset 999)


;;
;; ---
;;
;; Set column name for single value. Also set the dataset name.
;;


(tc/dataset 999 {:single-value-column-name "my-single-value"})
(tc/dataset 999 {:single-value-column-name ""
                 :dataset-name "Single value"})


;;
;; ---
;;
;; Sequence of pairs (first = column name, second = value(s)).
;;


(tc/dataset [[:A 33] [:B 5] [:C :a]])


;;
;; ---
;;
;; Not sequential values are repeated row-count number of times.
;;


(tc/dataset [[:A [1 2 3 4 5 6]] [:B "X"] [:C :a]])


;;
;; ---
;;
;; Dataset created from map (keys = column names, vals = value(s)). Works the same as sequence of pairs.
;;


(tc/dataset {:A 33})
(tc/dataset {:A [1 2 3]})
(tc/dataset {:A [3 4 5] :B "X"})


;;
;; ---
;;
;; You can put any value inside a column
;;


(tc/dataset {:A [[3 4 5] [:a :b]] :B "X"})


;;
;; ---
;;
;; Sequence of maps
;;


(tc/dataset [{:a 1 :b 3} {:b 2 :a 99}])
(tc/dataset [{:a 1 :b [1 2 3]} {:a 2 :b [3 4]}])


;;
;; ---
;;
;; Missing values are marked by `nil`
;;


(tc/dataset [{:a nil :b 1} {:a 3 :b 4} {:a 11}])


;;
;; ---
;;
;; Reading from arrays, by default `:as-rows`
;;


(-> (map int-array [[1 2] [3 4] [5 6]])
    (into-array)
    (tc/dataset))


;;
;; `:as-columns`
;;


(-> (map int-array [[1 2] [3 4] [5 6]])
    (into-array)
    (tc/dataset {:layout :as-columns}))


;;
;; `:as-rows` with names
;;


(-> (map int-array [[1 2] [3 4] [5 6]])
    (into-array)
    (tc/dataset {:layout :as-rows
                 :column-names [:a :b]}))


;;
;; ---
;;
;; Create dataset using macro `let-dataset` to simulate R `tibble` function. Each binding is converted into a column.
;;


(tc/let-dataset [x (range 1 6)
                 y 1
                 z (dfn/+ x y)])


;;
;; ---
;;
;; Import CSV file
;;


(tc/dataset "data/family.csv")


;;
;; ---
;;
;; Import from URL
;;


(defonce ds (tc/dataset "https://vega.github.io/vega-lite/examples/data/seattle-weather.csv"))


;;


ds


;;
;; #### Saving
;;
;; Export dataset to a file or output stream can be done by calling `tc/write!`. Function accepts:
;;
;; * dataset
;; * file name with one of the extensions: `.csv`, `.tsv`, `.csv.gz` and `.tsv.gz` or output stream
;; * options:
;;     - `:separator` - string or separator char.
;;


(tc/write! ds "output.tsv.gz")
(.exists (clojure.java.io/file "output.tsv.gz"))


;;
;; ##### Nippy
;;


(tc/write! DS "output.nippy.gz")


;;


(tc/dataset "output.nippy.gz")


;;
;; #### Dataset related functions
;;
;; Summary functions about the dataset like number of rows, columns and basic stats.
;;
;; ---
;;
;; Number of rows
;;


(tc/row-count ds)


;;
;; ---
;;
;; Number of columns
;;


(tc/column-count ds)


;;
;; ---
;;
;; Shape of the dataset, [row count, column count]
;;


(tc/shape ds)


;;
;; ---
;;
;; General info about dataset. There are three variants:
;;
;; * default - containing information about columns with basic statistics
;;     - `:basic` - just name, row and column count and information if dataset is a result of `group-by` operation
;;     - `:columns` - columns' metadata
;;


(tc/info ds)
(tc/info ds :basic)
(tc/info ds :columns)


;;
;; ---
;;
;; Getting a dataset name
;;


(tc/dataset-name ds)


;;
;; ---
;;
;; Setting a dataset name (operation is immutable).
;;


(->> "seattle-weather"
     (tc/set-dataset-name ds)
     (tc/dataset-name))


;;
;; #### Columns and rows
;;
;; Get columns and rows as sequences. `column`, `columns` and `rows` treat grouped dataset as regular one. See `Groups` to read more about grouped datasets.
;;
;; Possible result types:
;;
;; - `:as-seq` or `:as-seqs` - sequence of seqences (default)
;; - `:as-maps` - sequence of maps (rows)
;; - `:as-map` - map of sequences (columns)
;; - `:as-double-arrays` - array of double arrays
;;
;; ---
;;
;; Select column.
;;


(ds "wind")
(tc/column ds "date")


;;
;; ---
;;
;; Columns as sequence
;;


(take 2 (tc/columns ds))


;;
;; ---
;;
;; Columns as map
;;


(keys (tc/columns ds :as-map))


;;
;; ---
;;
;; Rows as sequence of sequences
;;


(take 2 (tc/rows ds))


;;
;; ---
;;
;; Select rows/columns as double-double-array
;;


(-> ds
    (tc/select-columns :type/numerical)
    (tc/head)
    (tc/rows :as-double-arrays))


;;


(-> ds
    (tc/select-columns :type/numerical)
    (tc/head)
    (tc/columns :as-double-arrays))


;;
;; ---
;;
;; Rows as sequence of maps
;;

(clay/capture-print
 (clojure.pprint/pprint (take 2 (tc/rows ds :as-maps))))

;;
;; #### Printing
;;
;; Dataset is printed using `dataset->str` or `print-dataset` functions. Options are the same as in `tech.ml.dataset/dataset-data->str`. Most important is `:print-line-policy` which can be one of the: `:single`, `:repl` or `:markdown`.
;;

(clay/capture-print
 (tc/print-dataset (tc/group-by DS :V1) {:print-line-policy :markdown}))


;;


(clay/capture-print
 (tc/print-dataset (tc/group-by DS :V1) {:print-line-policy :repl}))


;;


(clay/capture-print
 (tc/print-dataset (tc/group-by DS :V1) {:print-line-policy :single}))


;;
;; ### Group-by
;;
;; Grouping by is an operation which splits dataset into subdatasets and pack it into new special type of... dataset. I distinguish two types of dataset: regular dataset and grouped dataset. The latter is the result of grouping.
;;
;; Grouped dataset is annotated in by `:grouped?` meta tag and consist following columns:
;;
;; * `:name` - group name or structure
;; * `:group-id` - integer assigned to the group
;; * `:data` - groups as datasets
;;
;; Almost all functions recognize type of the dataset (grouped or not) and operate accordingly.
;;
;; You can't apply reshaping or join/concat functions on grouped datasets.
;;
;; #### Grouping
;;
;; Grouping is done by calling `group-by` function with arguments:
;;
;; * `ds` - dataset
;; * `grouping-selector` - what to use for grouping
;; * options:
;;     - `:result-type` - what to return:
;;         * `:as-dataset` (default) - return grouped dataset
;;         * `:as-indexes` - return rows ids (row number from original dataset)
;;         * `:as-map` - return map with group names as keys and subdataset as values
;;         * `:as-seq` - return sequens of subdatasets
;;     - `:select-keys` - list of the columns passed to a grouping selector function
;;
;; All subdatasets (groups) have set name as the group name, additionally `group-id` is in meta.
;;
;; Grouping can be done by:
;;
;; * single column name
;; * seq of column names
;; * map of keys (group names) and row indexes
;; * value returned by function taking row as map (limited to `:select-keys`)
;;
;; Note: currently dataset inside dataset is printed recursively so it renders poorly from markdown. So I will use `:as-seq` result type to show just group names and groups.
;;
;; ---
;;
;; List of columns in grouped dataset
;;


(-> DS
    (tc/group-by :V1)
    (tc/column-names))


;;
;; ---
;;
;; List of columns in grouped dataset treated as regular dataset
;;


(-> DS
    (tc/group-by :V1)
    (tc/as-regular-dataset)
    (tc/column-names))


;;
;;
;; ---
;;
;; Content of the grouped dataset
;;


(tc/columns (tc/group-by DS :V1) :as-map)


;;
;; ---
;;
;; Grouped dataset as map
;;


(keys (tc/group-by DS :V1 {:result-type :as-map}))


;;


(vals (tc/group-by DS :V1 {:result-type :as-map}))


;;
;; ---
;;
;; Group dataset as map of indexes (row ids)
;;


(tc/group-by DS :V1 {:result-type :as-indexes})


;;
;; ---
;;
;; Grouped datasets are printed as follows by default.
;;


(tc/group-by DS :V1)


;;
;; ---
;;
;; To get groups as sequence or a map can be done from grouped dataset using `groups->seq` and `groups->map` functions.
;;
;; Groups as seq can be obtained by just accessing `:data` column.
;;
;; I will use temporary dataset here.
;;


(let [ds (-> {"a" [1 1 2 2]
              "b" ["a" "b" "c" "d"]}
             (tc/dataset)
             (tc/group-by "a"))]
  (seq (ds :data))) ;; seq is not necessary but Markdown treats `:data` as command here


;;


(-> {"a" [1 1 2 2]
     "b" ["a" "b" "c" "d"]}
    (tc/dataset)
    (tc/group-by "a")
    (tc/groups->seq))


;;
;; ---
;;
;; Groups as map
;;


(-> {"a" [1 1 2 2]
     "b" ["a" "b" "c" "d"]}
    (tc/dataset)
    (tc/group-by "a")
    (tc/groups->map))


;;
;; ---
;;
;; Grouping by more than one column. You can see that group names are maps. When ungrouping is done these maps are used to restore column names.
;;


(tc/group-by DS [:V1 :V3] {:result-type :as-seq})


;;
;; ---
;;
;; Grouping can be done by providing just row indexes. This way you can assign the same row to more than one group.
;;


(tc/group-by DS {"group-a" [1 2 1 2]
                 "group-b" [5 5 5 1]} {:result-type :as-seq})


;;
;; ---
;;
;; You can group by a result of grouping function which gets row as map and should return group name. When map is used as a group name, ungrouping restore original column names.
;;


(tc/group-by DS (fn [row] (* (:V1 row)
                             (:V3 row))) {:result-type :as-seq})


;;
;; ---
;;
;; You can use any predicate on column to split dataset into two groups.
;;


(tc/group-by DS (comp #(< % 1.0) :V3) {:result-type :as-seq})


;;
;; ---
;;
;; `juxt` is also helpful
;;


(tc/group-by DS (juxt :V1 :V3) {:result-type :as-seq})


;;
;; ---
;;
;; `tech.ml.dataset` provides an option to limit columns which are passed to grouping functions. It's done for performance purposes.
;;


(tc/group-by DS identity {:result-type :as-seq
                          :select-keys [:V1]})


;;
;; #### Ungrouping
;;
;; Ungrouping simply concats all the groups into the dataset. Following options are possible
;;
;; * `:order?` - order groups according to the group name ascending order. Default: `false`
;; * `:add-group-as-column` - should group name become a column? If yes column is created with provided name (or `:$group-name` if argument is `true`). Default: `nil`.
;; * `:add-group-id-as-column` - should group id become a column? If yes column is created with provided name (or `:$group-id` if argument is `true`). Default: `nil`.
;; * `:dataset-name` - to name resulting dataset. Default: `nil` (_unnamed)
;;
;; If group name is a map, it will be splitted into separate columns. Be sure that groups (subdatasets) doesn't contain the same columns already.
;;
;; If group name is a vector, it will be splitted into separate columns. If you want to name them, set vector of target column names as `:add-group-as-column` argument.
;;
;; After ungrouping, order of the rows is kept within the groups but groups are ordered according to the internal storage.
;;
;; ---
;;
;; Grouping and ungrouping.
;;


(-> DS
    (tc/group-by :V3)
    (tc/ungroup))


;;
;; ---
;;
;; Groups sorted by group name and named.
;;


(-> DS
    (tc/group-by :V3)
    (tc/ungroup {:order? true
                 :dataset-name "Ordered by V3"}))


;;
;; ---
;;
;; Groups sorted descending by group name and named.
;;


(-> DS
    (tc/group-by :V3)
    (tc/ungroup {:order? :desc
                 :dataset-name "Ordered by V3 descending"}))


;;
;; ---
;;
;; Let's add group name and id as additional columns
;;


(-> DS
    (tc/group-by (comp #(< % 4) :V2))
    (tc/ungroup {:add-group-as-column true
                 :add-group-id-as-column true}))


;;
;; ---
;;
;; Let's assign different column names
;;


(-> DS
    (tc/group-by (comp #(< % 4) :V2))
    (tc/ungroup {:add-group-as-column "Is V2 less than 4?"
                 :add-group-id-as-column "group id"}))


;;
;; ---
;;
;; If we group by map, we can automatically create new columns out of group names.
;;


(-> DS
    (tc/group-by (fn [row] {"V1 and V3 multiplied" (* (:V1 row)
                                                      (:V3 row))
                            "V4 as lowercase" (clojure.string/lower-case (:V4 row))}))
    (tc/ungroup {:add-group-as-column true}))


;;
;; ---
;;
;; We can add group names without separation
;;


(-> DS
    (tc/group-by (fn [row] {"V1 and V3 multiplied" (* (:V1 row)
                                                      (:V3 row))
                            "V4 as lowercase" (clojure.string/lower-case (:V4 row))}))
    (tc/ungroup {:add-group-as-column "just map"
                 :separate? false}))


;;
;; ---
;;
;; The same applies to group names as sequences
;;


(-> DS
    (tc/group-by (juxt :V1 :V3))
    (tc/ungroup {:add-group-as-column "abc"}))


;;
;; ---
;;
;; Let's provide column names
;;


(-> DS
    (tc/group-by (juxt :V1 :V3))
    (tc/ungroup {:add-group-as-column ["v1" "v3"]}))


;;
;; ---
;;
;; Also we can supress separation
;;


(-> DS
    (tc/group-by (juxt :V1 :V3))
    (tc/ungroup {:separate? false
                 :add-group-as-column true}))
;; => _unnamed [9 5]:




;;
;; #### Other functions
;;
;; To check if dataset is grouped or not just use `grouped?` function.
;;


(tc/grouped? DS)


;;


(tc/grouped? (tc/group-by DS :V1))


;;
;; ---
;;
;; If you want to remove grouping annotation (to make all the functions work as with regular dataset) you can use `unmark-group` or `as-regular-dataset` (alias) functions.
;;
;; It can be important when you want to remove some groups (rows) from grouped dataset using `drop-rows` or something like that.
;;


(-> DS
    (tc/group-by :V1)
    (tc/as-regular-dataset)
    (tc/grouped?))


;;
;; You can also operate on grouped dataset as a regular one in case you want to access its columns using `without-grouping->` threading macro.
;;


(-> DS
    (tc/group-by [:V4 :V1])
    (tc/without-grouping->
     (tc/order-by (comp (juxt :V4 :V1) :name))))


;;
;; ---
;;
;; This is considered internal.
;;
;; If you want to implement your own mapping function on grouped dataset you can call `process-group-data` and pass function operating on datasets. Result should be a dataset to have ungrouping working.
;;


(-> DS
    (tc/group-by :V1)
    (tc/process-group-data #(str "Shape: " (vector (tc/row-count %) (tc/column-count %))))
    (tc/as-regular-dataset))


;;
;; ### Columns
;;
;; Column is a special `tech.ml.dataset` structure based on `tech.ml.datatype` library. For our purposes we cat treat columns as typed and named sequence bound to particular dataset.
;;
;; Type of the data is inferred from a sequence during column creation.
;;
;; #### Names
;;
;; To select dataset columns or column names `columns-selector` is used. `columns-selector` can be one of the following:
;;
;; * `:all` keyword - selects all columns
;; * column name - for single column
;; * sequence of column names - for collection of columns
;; * regex - to apply pattern on column names or datatype
;; * filter predicate - to filter column names or datatype
;; * `type` namespaced keyword for specific datatype or group of datatypes
;;
;; Column name can be anything.
;;
;; `column-names` function returns names according to `columns-selector` and optional `meta-field`. `meta-field` is one of the following:
;;
;; * `:name` (default) - to operate on column names
;; * `:datatype` - to operated on column types
;; * `:all` - if you want to process all metadata
;;
;; Datatype groups are:
;;
;; * `:type/numerical` - any numerical type
;; * `:type/float` - floating point number (`:float32` and `:float64`)
;; * `:type/integer` - any integer
;; * `:type/datetime` - any datetime type
;;
;; If qualified keyword starts with `:!type`, complement set is used.
;;
;; ---
;;
;; To select all column names you can use `column-names` function.
;;


(tc/column-names DS)


;;
;; or
;;


(tc/column-names DS :all)


;;
;; In case you want to select column which has name `:all` (or is sequence or map), put it into a vector. Below code returns empty sequence since there is no such column in the dataset.
;;


(tc/column-names DS [:all])


;;
;; ---
;;
;; Obviously selecting single name returns it's name if available
;;


(tc/column-names DS :V1)
(tc/column-names DS "no such column")


;;
;; ---
;;
;; Select sequence of column names.
;;


(tc/column-names DS [:V1 "V2" :V3 :V4 :V5])


;;
;; ---
;;
;; Select names based on regex, columns ends with `1` or `4`
;;


(tc/column-names DS #".*[14]")


;;
;; ---
;;
;; Select names based on regex operating on type of the column (to check what are the column types, call `(tc/info DS :columns)`. Here we want to get integer columns only.
;;


(tc/column-names DS #"^:int.*" :datatype)


;;
;; or
;;
;;


(tc/column-names DS :type/integer)


;;
;; ---
;;
;; And finally we can use predicate to select names. Let's select double precision columns.
;;


(tc/column-names DS #{:float64} :datatype)


;;
;; or
;;


(tc/column-names DS :type/float64)


;;
;; ---
;;
;; If you want to select all columns but given, use `complement` function. Works only on a predicate.
;;


(tc/column-names DS (complement #{:V1}))
(tc/column-names DS (complement #{:float64}) :datatype)
(tc/column-names DS :!type/float64)


;;
;; ---
;;
;; You can select column names based on all column metadata at once by using `:all` metadata selector. Below we want to select column names ending with `1` which have `long` datatype.
;;


(tc/column-names DS (fn [meta]
                      (and (= :int64 (:datatype meta))
                           (clojure.string/ends-with? (:name meta) "1"))) :all)


;;
;; #### Select
;;
;; `select-columns` creates dataset with columns selected by `columns-selector` as described above. Function works on regular and grouped dataset.
;;
;; ---
;;
;; Select only float64 columns
;;


(tc/select-columns DS #(= :float64 %) :datatype)


;; or
;;


(tc/select-columns DS :type/float64)


;;
;;
;; ---
;;
;; Select all but `:V1` columns
;;


(tc/select-columns DS (complement #{:V1}))


;;
;; ---
;;
;; If we have grouped data set, column selection is applied to every group separately.
;;


(-> DS
    (tc/group-by :V1)
    (tc/select-columns [:V2 :V3])
    (tc/groups->map))


;;
;; #### Drop
;;
;; `drop-columns` creates dataset with removed columns.
;;
;; ---
;;
;; Drop float64 columns
;;


(tc/drop-columns DS #(= :float64 %) :datatype)


;;
;; or
;;


(tc/drop-columns DS :type/float64)


;;
;; ---
;;
;; Drop all columns but `:V1` and `:V2`
;;


(tc/drop-columns DS (complement #{:V1 :V2}))


;;
;; ---
;;
;; If we have grouped data set, column selection is applied to every group separately. Selected columns are dropped.
;;


(-> DS
    (tc/group-by :V1)
    (tc/drop-columns [:V2 :V3])
    (tc/groups->map))


;;
;; #### Rename
;;
;; If you want to rename colums use `rename-columns` and pass map where keys are old names, values new ones.
;;
;; You can also pass mapping function with optional columns-selector
;;


(tc/rename-columns DS {:V1 "v1"
                       :V2 "v2"
                       :V3 [1 2 3]
                       :V4 (Object.)})


;;
;; ---
;;
;; Map all names with function
;;


(tc/rename-columns DS (comp str second name))


;;
;; ---
;;
;; Map selected names with function
;;


(tc/rename-columns DS [:V1 :V3] (comp str second name))


;;
;; ---
;;
;; Function works on grouped dataset
;;


(-> DS
    (tc/group-by :V1)
    (tc/rename-columns {:V1 "v1"
                        :V2 "v2"
                        :V3 [1 2 3]
                        :V4 (Object.)})
    (tc/groups->map))


;;
;;
;; #### Add or update
;;
;; To add (or replace existing) column call `add-column` function. Function accepts:
;;
;; * `ds` - a dataset
;; * `column-name` - if it's existing column name, column will be replaced
;; * `column` - can be column (from other dataset), sequence, single value or function. Too big columns are always trimmed. Too small are cycled or extended with missing values (according to `size-strategy` argument)
;; * `size-strategy` (optional) - when new column is shorter than dataset row count, following strategies are applied:
;;     - `:cycle` - repeat data
;;     - `:na` - append missing values
;;     - `:strict` - (default) throws an exception when sizes mismatch
;;
;; Function works on grouped dataset.
;;
;; ---
;;
;; Add single value as column
;;


(tc/add-column DS :V5 "X")


;;
;; ---
;;
;; Replace one column (column is trimmed)
;;


(tc/add-column DS :V1 (repeatedly rand))


;;
;; ---
;;
;; Copy column
;;


(tc/add-column DS :V5 (DS :V1))


;;
;; ---
;;
;; When function is used, argument is whole dataset and the result should be column, sequence or single value
;;


(tc/add-column DS :row-count tc/row-count)


;;
;; ---
;;
;; Above example run on grouped dataset, applies function on each group separately.
;;


(-> DS
    (tc/group-by :V1)
    (tc/add-column :row-count tc/row-count)
    (tc/ungroup))


;;
;; ---
;;
;; When column which is added is longer than row count in dataset, column is trimmed. When column is shorter, it's cycled or missing values are appended.
;;


(tc/add-column DS :V5 [:r :b] :cycle)


;;


(tc/add-column DS :V5 [:r :b] :na)


;;
;; Exception is thrown when `:strict` (default) strategy is used and column size is not equal row count
;;


(try
  (tc/add-column DS :V5 [:r :b])
  (catch Exception e (str "Exception caught: "(ex-message e))))


;;
;; ---
;;
;; Tha same applies for grouped dataset
;;


(-> DS
    (tc/group-by :V3)
    (tc/add-column :V5 [:r :b] :na)
    (tc/ungroup))


;;
;; ---
;;
;; Let's use other column to fill groups
;;


(-> DS
    (tc/group-by :V3)
    (tc/add-column :V5 (DS :V2) :cycle)
    (tc/ungroup))


;;
;; ---
;;
;; In case you want to add or update several columns you can call `add-columns` and provide map where keys are column names, vals are columns.
;;


(tc/add-columns DS {:V1 #(map inc (% :V1))
                    :V5 #(map (comp keyword str) (% :V4))
                    :V6 11})


;;
;; #### Update
;;
;; If you want to modify specific column(s) you can call `update-columns`. Arguments:
;;
;; * dataset
;; * one of:
;;     - `columns-selector` and function (or sequence of functions)
;;     - map where keys are column names and vals are function
;;
;; Functions accept column and have to return column or sequence
;;
;; ---
;;
;; Reverse of columns
;;


(tc/update-columns DS :all reverse)


;;
;; ---
;;
;; Apply dec/inc on numerical columns
;;


(tc/update-columns DS :type/numerical [(partial map dec)
                                       (partial map inc)])


;;
;; ---
;;
;; You can also assign a function to a column by packing operations into the map.
;;


(tc/update-columns DS {:V1 reverse
                       :V2 (comp shuffle seq)})


;;
;;
;; #### Map
;;
;; The other way of creating or updating column is to map rows as regular `map` function. The arity of mapping function should be the same as number of selected columns.
;;
;; Arguments:
;;
;; * `ds` - dataset
;; * `column-name` - target column name
;; * `columns-selector` - columns selected
;; * `map-fn` - mapping function
;;
;; ---
;;
;; Let's add numerical columns together
;;


(tc/map-columns DS
                :sum-of-numbers
                (tc/column-names DS  #{:int64 :float64} :datatype)
                (fn [& rows]
                  (reduce + rows)))


;;
;; The same works on grouped dataset
;;


(-> DS
    (tc/group-by :V4)
    (tc/map-columns :sum-of-numbers
                    (tc/column-names DS  #{:int64 :float64} :datatype)
                    (fn [& rows]
                      (reduce + rows)))
    (tc/ungroup))


;;
;; #### Reorder
;;
;; To reorder columns use columns selectors to choose what columns go first. The unseleted columns are appended to the end.
;;


(tc/reorder-columns DS :V4 [:V3 :V2])


;;
;; ---
;;
;; This function doesn't let you select meta field, so you have to call `column-names` in such case. Below we want to add integer columns at the end.
;;


(tc/reorder-columns DS (tc/column-names DS (complement #{:int64}) :datatype))


;;
;; #### Type conversion
;;
;; To convert column into given datatype can be done using `convert-types` function. Not all the types can be converted automatically also some types require slow parsing (every conversion from string). In case where conversion is not possible you can pass conversion function.
;;
;; Arguments:
;;
;; * `ds` - dataset
;; * Two options:
;;     - `coltype-map` in case when you want to convert several columns, keys are column names, vals are new types
;;     - `column-selector` and `new-types` - column name and new datatype (or datatypes as sequence)
;;
;; `new-types` can be:
;;
;; * a type like `:int64` or `:string` or sequence of types
;; * or sequence of pair of datetype and conversion function
;;
;; After conversion additional infomation is given on problematic values.
;;
;; The other conversion is casting column into java array (`->array`) of the type column or provided as argument. Grouped dataset returns sequence of arrays.
;;
;; ---
;;
;; Basic conversion
;;


(-> DS
    (tc/convert-types :V1 :float64)
    (tc/info :columns))


;;
;; ---
;;
;; Using custom converter. Let's treat `:V4` as haxadecimal values. See that this way we can map column to any value.
;;


(-> DS
    (tc/convert-types :V4 [[:int16 #(Integer/parseInt % 16)]]))


;;
;; ---
;;
;; You can process several columns at once
;;


(-> DS
    (tc/convert-types {:V1 :float64
                       :V2 :object
                       :V3 [:boolean #(< % 1.0)]
                       :V4 :object})
    (tc/info :columns))


;;
;; ---
;;
;; Convert one type into another
;;


(-> DS
    (tc/convert-types :type/numerical :int16)
    (tc/info :columns))


;;
;; ---
;;
;; Function works on the grouped dataset
;;


(-> DS
    (tc/group-by :V1)
    (tc/convert-types :V1 :float32)
    (tc/ungroup)
    (tc/info :columns))


;;
;; ---
;;
;; Double array conversion.
;;


(tc/->array DS :V1)


;;
;; ---
;;
;; Function also works on grouped dataset
;;


(-> DS
    (tc/group-by :V3)
    (tc/->array :V2))


;;
;; ---
;;
;; You can also cast the type to the other one (if casting is possible):
;;


(tc/->array DS :V4 :string)
(tc/->array DS :V1 :float32)


;;
;; ### Rows
;;
;; Rows can be selected or dropped using various selectors:
;;
;; * row id(s) - row index as number or seqence of numbers (first row has index `0`, second `1` and so on)
;; * sequence of true/false values
;; * filter by predicate (argument is row as a map)
;;
;; When predicate is used you may want to limit columns passed to the function (`select-keys` option).
;;
;; Additionally you may want to precalculate some values which will be visible for predicate as additional columns. It's done internally by calling `add-columns` on a dataset. `:pre` is used as a column definitions.
;;
;; #### Select
;;
;; Select fifth row
;;


(tc/select-rows DS 4)


;;
;; ---
;;
;; Select 3 rows
;;


(tc/select-rows DS [1 4 5])


;;
;; ---
;;
;; Select rows using sequence of true/false values
;;


(tc/select-rows DS [true nil nil true])


;;
;; ---
;;
;; Select rows using predicate
;;


(tc/select-rows DS (comp #(< % 1) :V3))


;;
;; ---
;;
;; The same works on grouped dataset, let's select first row from every group.
;;


(-> DS
    (tc/group-by :V1)
    (tc/select-rows 0)
    (tc/ungroup))


;;
;; ---
;;
;; If you want to select `:V2` values which are lower than or equal mean in grouped dataset you have to precalculate it using `:pre`.
;;


(-> DS
    (tc/group-by :V4)
    (tc/select-rows (fn [row] (<= (:V2 row) (:mean row)))
                    {:pre {:mean #(tech.v3.datatype.functional/mean (% :V2))}})
    (tc/ungroup))


;;
;; #### Drop
;;
;; `drop-rows` removes rows, and accepts exactly the same parameters as `select-rows`
;;
;; ---
;;
;; Drop values lower than or equal `:V2` column mean in grouped dataset.
;;


(-> DS
    (tc/group-by :V4)
    (tc/drop-rows (fn [row] (<= (:V2 row) (:mean row)))
                  {:pre {:mean #(tech.v3.datatype.functional/mean (% :V2))}})
    (tc/ungroup))


;;
;; #### Other
;;
;; There are several function to select first, last, random rows, or display head, tail of the dataset. All functions work on grouped dataset.
;;
;; All random functions accept `:seed` as an option if you want to fix returned result.
;;
;; ---
;;
;; First row
;;


(tc/first DS)


;;
;; ---
;;
;; Last row
;;


(tc/last DS)


;;
;; ---
;;
;; Random row (single)
;;


(tc/rand-nth DS)


;;
;; ---
;;
;; Random row (single) with seed
;;


(tc/rand-nth DS {:seed 42})


;;
;; ---
;;
;; Random `n` (default: row count) rows with repetition.
;;


(tc/random DS)


;;
;; ---
;;
;; Five random rows with repetition
;;


(tc/random DS 5)


;;
;; ---
;;
;; Five random, non-repeating rows
;;


(tc/random DS 5 {:repeat? false})


;;
;; ---
;;
;; Five random, with seed
;;


(tc/random DS 5 {:seed 42})


;;
;;
;; ---
;;
;; Shuffle dataset
;;


(tc/shuffle DS)


;;
;; ---
;;
;; Shuffle with seed
;;


(tc/shuffle DS {:seed 42})


;;
;;
;; ---
;;
;; First `n` rows (default 5)
;;


(tc/head DS)


;;
;; ---
;;
;; Last `n` rows (default 5)
;;


(tc/tail DS)


;;
;; ---
;;
;; `by-rank` calculates rank on column(s). It's base on [R rank()](https://www.rdocumentation.org/packages/base/versions/3.6.1/topics/rank) with addition of `:dense` (default) tie strategy which give consecutive rank numbering.
;;
;; `:desc?` options (default: `true`) sorts input with descending order, giving top values under `0` value.
;;
;; `rank` is zero based and is defined at `tablecloth.api.utils` namespace.
;;
;; ---
;;


(tc/by-rank DS :V3 zero?) ;; most V3 values


;;


(tc/by-rank DS :V3 zero? {:desc? false}) ;; least V3 values


;;
;; ---
;;
;; Rank also works on multiple columns
;;


(tc/by-rank DS [:V1 :V3] zero? {:desc? false})


;;
;; ---
;;
;; Select 5 random rows from each group
;;


(-> DS
    (tc/group-by :V4)
    (tc/random 5)
    (tc/ungroup))


;;
;; ### Aggregate
;;
;; Aggregating is a function which produces single row out of dataset.
;;
;; Aggregator is a function or sequence or map of functions which accept dataset as an argument and result single value, sequence of values or map.
;;
;; Where map is given as an input or result, keys are treated as column names.
;;
;; Grouped dataset is ungrouped after aggreation. This can be turned off by setting `:ungroup` to false. In case you want to pass additional ungrouping parameters add them to the options.
;;
;; By default resulting column names are prefixed with `summary` prefix (set it with `:default-column-name-prefix` option).
;;
;; ---
;;
;; Let's calculate mean of some columns
;;


(tc/aggregate DS #(reduce + (% :V2)))


;;
;; ---
;;
;; Let's give resulting column a name.
;;


(tc/aggregate DS {:sum-of-V2 #(reduce + (% :V2))})


;;
;; ---
;;
;; Sequential result is spread into separate columns
;;


(tc/aggregate DS #(take 5(% :V2)))


;;
;; ---
;;
;; You can combine all variants and rename default prefix
;;


(tc/aggregate DS [#(take 3 (% :V2))
                  (fn [ds] {:sum-v1 (reduce + (ds :V1))
                            :prod-v3 (reduce * (ds :V3))})] {:default-column-name-prefix "V2-value"})


;;
;; ---
;;
;; Processing grouped dataset
;;


(-> DS
    (tc/group-by [:V4])
    (tc/aggregate [#(take 3 (% :V2))
                   (fn [ds] {:sum-v1 (reduce + (ds :V1))
                             :prod-v3 (reduce * (ds :V3))})] {:default-column-name-prefix "V2-value"}))


;;
;; Result of aggregating is automatically ungrouped, you can skip this step by stetting `:ungroup` option to `false`.
;;


(-> DS
    (tc/group-by [:V3])
    (tc/aggregate [#(take 3 (% :V2))
                   (fn [ds] {:sum-v1 (reduce + (ds :V1))
                             :prod-v3 (reduce * (ds :V3))})] {:default-column-name-prefix "V2-value"
                                                              :ungroup? false}))


;;
;; #### Column
;;
;; You can perform columnar aggreagation also. `aggregate-columns` selects columns and apply aggregating function (or sequence of functions) for each column separately.
;;


(tc/aggregate-columns DS [:V1 :V2 :V3] #(reduce + %))


;;
;; ---
;;


(tc/aggregate-columns DS [:V1 :V2 :V3] [#(reduce + %)
                                        #(reduce max %)
                                        #(reduce * %)])


;;
;; ---
;;


(-> DS
    (tc/group-by [:V4])
    (tc/aggregate-columns [:V1 :V2 :V3] #(reduce + %)))


;;
;;
;; ### Order
;;
;; Ordering can be done by column(s) or any function operating on row. Possible order can be:
;;
;; * `:asc` for ascending order (default)
;; * `:desc` for descending order
;; * custom comparator
;;
;; `:select-keys` limits row map provided to ordering functions.
;;
;; ---
;;
;; Order by single column, ascending
;;


(tc/order-by DS :V1)


;;
;; ---
;;
;; Descending order
;;


(tc/order-by DS :V1 :desc)


;;
;; ---
;;
;; Order by two columns
;;


(tc/order-by DS [:V1 :V2])


;;
;; ---
;;
;; Use different orders for columns
;;


(tc/order-by DS [:V1 :V2] [:asc :desc])


;;


(tc/order-by DS [:V1 :V2] [:desc :desc])


;;


(tc/order-by DS [:V1 :V3] [:desc :asc])


;;
;; ---
;;
;; Custom function can be used to provided ordering key. Here order by `:V4` descending, then by product of other columns ascending.
;;


(tc/order-by DS [:V4 (fn [row] (* (:V1 row)
                                  (:V2 row)
                                  (:V3 row)))] [:desc :asc])


;;
;; ---
;;
;; Custom comparator also can be used in case objects are not comparable by default. Let's define artificial one: if Euclidean distance is lower than 2, compare along `z` else along `x` and `y`. We use first three columns for that.
;;


(defn dist
  [v1 v2]
  (->> v2
       (map - v1)
       (map #(* % %))
       (reduce +)
       (Math/sqrt)))


;;


(tc/order-by DS [:V1 :V2 :V3] (fn [[x1 y1 z1 :as v1] [x2 y2 z2 :as v2]]
                                (let [d (dist v1 v2)]
                                  (if (< d 2.0)
                                    (compare z1 z2)
                                    (compare [x1 y1] [x2 y2])))))


;;
;; ### Unique
;;
;; Remove rows which contains the same data. By default `unique-by` removes duplicates from whole dataset. You can also pass list of columns or functions (similar as in `group-by`) to remove duplicates limited by them. Default strategy is to keep the first row. More strategies below.
;;
;; `unique-by` works on groups
;;
;; ---
;;
;; Remove duplicates from whole dataset
;;


(tc/unique-by DS)


;;
;; ---
;;
;; Remove duplicates from each group selected by column.
;;


(tc/unique-by DS :V1)


;;
;; ---
;;
;; Pair of columns
;;


(tc/unique-by DS [:V1 :V3])


;;
;; ---
;;
;; Also function can be used, split dataset by modulo 3 on columns `:V2`
;;


(tc/unique-by DS (fn [m] (mod (:V2 m) 3)))


;;
;; ---
;;
;; The same can be achived with `group-by`
;;


(-> DS
    (tc/group-by (fn [m] (mod (:V2 m) 3)))
    (tc/first)
    (tc/ungroup))


;;
;; ---
;;
;; Grouped dataset
;;


(-> DS
    (tc/group-by :V4)
    (tc/unique-by :V1)
    (tc/ungroup))


;;
;; #### Strategies
;;
;; There are 4 strategies defined:
;;
;; * `:first` - select first row (default)
;; * `:last` - select last row
;; * `:random` - select random row
;; * any function - apply function to a columns which are subject of uniqueness
;;
;; ---
;;
;; Last
;;


(tc/unique-by DS :V1 {:strategy :last})


;;
;; ---
;;
;; Random
;;


(tc/unique-by DS :V1 {:strategy :random})


;;
;; ---
;;
;; Pack columns into vector
;;


(tc/unique-by DS :V4 {:strategy vec})


;;
;; ---
;;
;; Sum columns
;;


(tc/unique-by DS :V4 {:strategy (partial reduce +)})


;;
;; ---
;;
;; Group by function and apply functions
;;


(tc/unique-by DS (fn [m] (mod (:V2 m) 3)) {:strategy vec})


;;
;; ---
;;
;; Grouped dataset
;;


(-> DS
    (tc/group-by :V1)
    (tc/unique-by (fn [m] (mod (:V2 m) 3)) {:strategy vec})
    (tc/ungroup {:add-group-as-column :from-V1}))


;;
;; ### Missing
;;
;; When dataset contains missing values you can select or drop rows with missing values or replace them using some strategy.
;;
;; `column-selector` can be used to limit considered columns
;;
;; Let's define dataset which contains missing values
;;


(def DSm (tc/dataset {:V1 (take 9 (cycle [1 2 nil]))
                      :V2 (range 1 10)
                      :V3 (take 9 (cycle [0.5 1.0 nil 1.5]))
                      :V4 (take 9 (cycle ["A" "B" "C"]))}))


;;


DSm


;;
;; #### Select
;;
;; Select rows with missing values
;;


(tc/select-missing DSm)


;;
;; ---
;;
;; Select rows with missing values in `:V1`
;;


(tc/select-missing DSm :V1)


;;
;; ---
;;
;; The same with grouped dataset
;;


(-> DSm
    (tc/group-by :V4)
    (tc/select-missing :V3)
    (tc/ungroup))


;;
;; #### Drop
;;
;; Drop rows with missing values
;;


(tc/drop-missing DSm)


;;
;; ---
;;
;; Drop rows with missing values in `:V1`
;;


(tc/drop-missing DSm :V1)


;;
;; ---
;;
;; The same with grouped dataset
;;


(-> DSm
    (tc/group-by :V4)
    (tc/drop-missing :V1)
    (tc/ungroup))


;;
;; #### Replace
;;
;; Missing values can be replaced using several strategies. `replace-missing` accepts:
;;
;; * dataset
;; * column selector, default: `:all`
;; * strategy, default: `:nearest`
;; * value (optional)
;;     - single value
;;     - sequence of values (cycled)
;;     - function, applied on column(s) with stripped missings
;;
;; Strategies are:
;;
;; * `:value` - replace with given value
;; * `:up` - copy values up and then down for missing values at the end
;; * `:down` - copy values down and then up for missing values at the beginning
;; * `:mid` or `:nearest` - copy values around known values
;; * `:midpoint` - use average value from previous and next non-missing
;; * `:lerp` - trying to lineary approximate values, works for numbers and datetime, otherwise applies `:nearest`. For numbers always results in `float` datatype.
;;
;; Let's define special dataset here:
;;


(def DSm2 (tc/dataset {:a [nil nil nil 1.0 2  nil nil nil nil  nil 4   nil  11 nil nil]
                       :b [2   2   2 nil nil nil nil nil nil 13   nil   3  4  5 5]}))


;;


DSm2


;;
;; ---
;;
;; Replace missing with default strategy for all columns
;;


(tc/replace-missing DSm2)


;;
;; ---
;;
;; Replace missing with single value in whole dataset
;;


(tc/replace-missing DSm2 :all :value 999)


;;
;; ---
;;
;; Replace missing with single value in `:a` column
;;


(tc/replace-missing DSm2 :a :value 999)


;;
;; ---
;;
;; Replace missing with sequence in `:a` column
;;


(tc/replace-missing DSm2 :a :value [-999 -998 -997])


;;
;; ---
;;
;; Replace missing with a function (mean)
;;


(tc/replace-missing DSm2 :a :value tech.v3.datatype.functional/mean)


;;
;; ---
;;
;; Using `:down` strategy, fills gaps with values from above. You can see that if missings are at the beginning, the are filled with first value
;;


(tc/replace-missing DSm2 [:a :b] :down)


;;
;; ---
;;
;; To fix above issue you can provide value
;;


(tc/replace-missing DSm2 [:a :b] :down 999)


;;
;; ---
;;
;; The same applies for `:up` strategy which is opposite direction.
;;


(tc/replace-missing DSm2 [:a :b] :up)


;;
;; ---
;;
;; The same applies for `:up` strategy which is opposite direction.
;;


(tc/replace-missing DSm2 [:a :b] :midpoint)


;;
;; ---
;;
;; We can use a function which is applied after applying `:up` or `:down`
;;


(tc/replace-missing DSm2 [:a :b] :down tech.v3.datatype.functional/mean)


;;
;; ---
;;
;; Lerp tries to apply linear interpolation of the values
;;


(tc/replace-missing DSm2 [:a :b] :lerp)


;;
;; ---
;;
;; Lerp works also on dates
;;


(-> (tc/dataset {:dt [(java.time.LocalDateTime/of 2020 1 1 11 22 33)
                      nil nil nil nil nil nil nil
                      (java.time.LocalDateTime/of 2020 10 1 1 1 1)]})
    (tc/replace-missing :lerp))


;;
;; #### Inject
;;
;; When your column contains not continuous data range you can fill up with lacking values. Arguments:
;;
;; * dataset
;; * column name
;; * expected step (`max-span`, milliseconds in case of datetime column)
;; * (optional) `missing-strategy` - how to replace missing, default `:down` (set to `nil` if none)
;; * (optional) `missing-value` - optional value for replace missing
;;
;; ---
;;


(-> (tc/dataset {:a [1 2 9]
                 :b [:a :b :c]})
    (tc/fill-range-replace :a 1))


;;
;; ### Join/Separate Columns
;;
;; Joining or separating columns are operations which can help to tidy messy dataset.
;;
;; * `join-columns` joins content of the columns (as string concatenation or other structure) and stores it in new column
;; * `separate-column` splits content of the columns into set of new columns
;;
;; #### Join
;;
;; `join-columns` accepts:
;;
;; * dataset
;; * column selector (as in `select-columns`)
;; * options
;;     - `:separator` (default `\"-\"`)
;;     - `:drop-columns?` - whether to drop source columns or not (default `true`)
;;     - `:result-type`
;;         * `:map` - packs data into map
;;         * `:seq` - packs data into sequence
;;         * `:string` - join strings with separator (default)
;;         * or custom function which gets row as a vector
;;     - `:missing-subst` - substitution for missing value
;;
;; ---
;;
;; Default usage. Create `:joined` column out of other columns.
;;


(tc/join-columns DSm :joined [:V1 :V2 :V4])


;;
;; ---
;;
;; Without dropping source columns.
;;


(tc/join-columns DSm :joined [:V1 :V2 :V4] {:drop-columns? false})


;;
;; ---
;;
;; Let's replace missing value with \"NA\" string.
;;


(tc/join-columns DSm :joined [:V1 :V2 :V4] {:missing-subst "NA"})


;;
;; ---
;;
;; We can use custom separator.
;;


(tc/join-columns DSm :joined [:V1 :V2 :V4] {:separator "/"
                                            :missing-subst "."})


;;
;; ---
;;
;; Or even sequence of separators.
;;


(tc/join-columns DSm :joined [:V1 :V2 :V4] {:separator ["-" "/"]
                                            :missing-subst "."})


;;
;; ---
;;
;; The other types of results, map:
;;


(tc/join-columns DSm :joined [:V1 :V2 :V4] {:result-type :map})


;;
;; ---
;;
;; Sequence
;;


(tc/join-columns DSm :joined [:V1 :V2 :V4] {:result-type :seq})


;;
;; ---
;;
;; Custom function, calculate hash
;;


(tc/join-columns DSm :joined [:V1 :V2 :V4] {:result-type hash})


;;
;; ---
;;
;; Grouped dataset
;;


(-> DSm
    (tc/group-by :V4)
    (tc/join-columns :joined [:V1 :V2 :V4])
    (tc/ungroup))


;;
;; ---
;;
;; ##### Tidyr examples
;;
;; [source](https://tidyr.tidyverse.org/reference/unite.html)
;;


(def df (tc/dataset {:x ["a" "a" nil nil]
                     :y ["b" nil "b" nil]}))


;;


df


;;
;; ---
;;


(tc/join-columns df "z" [:x :y] {:drop-columns? false
                                 :missing-subst "NA"
                                 :separator "_"})


;;
;; ---
;;


(tc/join-columns df "z" [:x :y] {:drop-columns? false
                                 :separator "_"})


;;
;;
;; #### Separate
;;
;; Column can be also separated into several other columns using string as separator, regex or custom function. Arguments:
;;
;; * dataset
;; * source column
;; * target columns - can be `nil` or `:infer` if `separator` returns map
;; * separator as:
;;     - string - it's converted to regular expression and passed to `clojure.string/split` function
;;     - regex
;;     - or custom function (default: identity)
;; * options
;;     - `:drop-columns?` - whether drop source column(s) or not (default: `true` or `:all` in case of empty `target-columns`). When set to `:all` keeps only separation result.
;;     - `:missing-subst` - values which should be treated as missing, can be set, sequence, value or function (default: `\"\"`)
;;
;; Custom function (as separator) should return seqence of values for given value.
;;
;; ---
;;
;; Separate float into integer and factional values
;;


(tc/separate-column DS :V3 [:int-part :frac-part] (fn [^double v]
                                                    [(int (quot v 1.0))
                                                     (mod v 1.0)]))


;;
;; ---
;;
;; Source column can be kept
;;


(tc/separate-column DS :V3 [:int-part :frac-part] (fn [^double v]
                                                    [(int (quot v 1.0))
                                                     (mod v 1.0)]) {:drop-column? false})


;;
;; ---
;;
;; We can treat `0` or `0.0` as missing value
;;


(tc/separate-column DS :V3 [:int-part :frac-part] (fn [^double v]
                                                    [(int (quot v 1.0))
                                                     (mod v 1.0)]) {:missing-subst [0 0.0]})


;;
;; ---
;;
;; Works on grouped dataset
;;


(-> DS
    (tc/group-by :V4)
    (tc/separate-column :V3 [:int-part :fract-part] (fn [^double v]
                                                      [(int (quot v 1.0))
                                                       (mod v 1.0)]))
    (tc/ungroup))


;;
;; ---
;;
;; Separate using separator returning sequence of maps, in this case we drop all other columns.
;;


(tc/separate-column DS :V3 (fn [^double v]
                             {:int-part (int (quot v 1.0))
                              :fract-part (mod v 1.0)}))


;;
;; Keeping all columns
;;


(tc/separate-column DS :V3 nil (fn [^double v]
                                 {:int-part (int (quot v 1.0))
                                  :fract-part (mod v 1.0)}) {:drop-column? false})


;;
;;
;; ---
;;
;; Join and separate together.
;;


(-> DSm
    (tc/join-columns :joined [:V1 :V2 :V4] {:result-type :map})
    (tc/separate-column :joined [:v1 :v2 :v4] (juxt :V1 :V2 :V4)))


;;


(-> DSm
    (tc/join-columns :joined [:V1 :V2 :V4] {:result-type :seq})
    (tc/separate-column :joined [:v1 :v2 :v4] identity))


;;
;; ##### Tidyr examples
;;
;; [separate source](https://tidyr.tidyverse.org/reference/separate.html)
;; [extract source](https://tidyr.tidyverse.org/reference/extract.html)
;;


(def df-separate (tc/dataset {:x [nil "a.b" "a.d" "b.c"]}))
(def df-separate2 (tc/dataset {:x ["a" "a b" nil "a b c"]}))
(def df-separate3 (tc/dataset {:x ["a?b" nil "a.b" "b:c"]}))
(def df-extract (tc/dataset {:x [nil "a-b" "a-d" "b-c" "d-e"]}))


;;


df-separate


;;


df-separate2


;;


df-separate3


;;


df-extract


;;
;; ---
;;


(tc/separate-column df-separate :x [:A :B] "\\.")


;;
;; ---
;;
;; You can drop columns after separation by setting `nil` as a name. We need second value here.
;;


(tc/separate-column df-separate :x [nil :B] "\\.")


;;
;; ---
;;
;; Extra data is dropped
;;


(tc/separate-column df-separate2 :x ["a" "b"] " ")


;;
;; ---
;;
;; Split with regular expression
;;


(tc/separate-column df-separate3 :x ["a" "b"] "[?\\.:]")


;;
;; ---
;;
;; Or just regular expression to extract values
;;


(tc/separate-column df-separate3 :x ["a" "b"] #"(.).(.)")


;;
;; ---
;;
;; Extract first value only
;;


(tc/separate-column df-extract :x ["A"] "-")


;;
;; ---
;;
;; Split with regex
;;


(tc/separate-column df-extract :x ["A" "B"] #"(\p{Alnum})-(\p{Alnum})")


;;
;; ---
;;
;; Only `a,b,c,d` strings
;;


(tc/separate-column df-extract :x ["A" "B"] #"([a-d]+)-([a-d]+)")


;;
;; ### Fold/Unroll Rows
;;
;; To pack or unpack the data into single value you can use `fold-by` and `unroll` functions.
;;
;; `fold-by` groups dataset and packs columns data from each group separately into desired datastructure (like vector or sequence). `unroll` does the opposite.
;;
;; #### Fold-by
;;
;; Group-by and pack columns into vector
;;


(tc/fold-by DS [:V3 :V4 :V1])


;;
;; ---
;;
;; You can pack several columns at once.
;;


(tc/fold-by DS [:V4])


;;
;; ---
;;
;; You can use custom packing function
;;


(tc/fold-by DS [:V4] seq)


;;
;; or
;;


(tc/fold-by DS [:V4] set)


;;
;; ---
;;
;; This works also on grouped dataset
;;


(-> DS
    (tc/group-by :V1)
    (tc/fold-by :V4)
    (tc/ungroup))


;;
;; #### Unroll
;;
;; `unroll` unfolds sequences stored in data, multiplying other ones when necessary. You can unroll more than one column at once (folded data should have the same size!).
;;
;; Options:
;;
;; * `:indexes?` if true (or column name), information about index of unrolled sequence is added.
;; * `:datatypes` list of datatypes which should be applied to restored columns, a map
;;
;; ---
;;
;; Unroll one column
;;


(tc/unroll (tc/fold-by DS [:V4]) [:V1])


;;
;; ---
;;
;; Unroll all folded columns
;;


(tc/unroll (tc/fold-by DS [:V4]) [:V1 :V2 :V3])


;;
;; ---
;;
;; Unroll one by one leads to cartesian product
;;


(-> DS
    (tc/fold-by [:V4 :V1])
    (tc/unroll [:V2])
    (tc/unroll [:V3]))


;;
;; ---
;;
;; You can add indexes
;;


(tc/unroll (tc/fold-by DS [:V1]) [:V4 :V2 :V3] {:indexes? true})


;;


(tc/unroll (tc/fold-by DS [:V1]) [:V4 :V2 :V3] {:indexes? "vector idx"})


;;
;; ---
;;
;; You can also force datatypes
;;


(-> DS
    (tc/fold-by [:V1])
    (tc/unroll [:V4 :V2 :V3] {:datatypes {:V4 :string
                                          :V2 :int16
                                          :V3 :float32}})
    (tc/info :columns))


;;
;; ---
;;
;; This works also on grouped dataset
;;


(-> DS
    (tc/group-by :V1)
    (tc/fold-by [:V1 :V4])
    (tc/unroll :V3 {:indexes? true})
    (tc/ungroup))


;;
;; ### Reshape
;;
;; Reshaping data provides two types of operations:
;;
;; * `pivot->longer` - converting columns to rows
;; * `pivot->wider` - converting rows to columns
;;
;; Both functions are inspired on [tidyr](https://tidyr.tidyverse.org/articles/pivot.html) R package and provide almost the same functionality.
;;
;; All examples are taken from mentioned above documentation.
;;
;; Both functions work only on regular dataset.
;;
;; #### Longer
;;
;; `pivot->longer` converts columns to rows. Column names are treated as data.
;;
;; Arguments:
;;
;; * dataset
;; * columns selector
;; * options:
;;     - `:target-columns` - names of the columns created or columns pattern (see below) (default: `:$column`)
;;     - `:value-column-name` - name of the column for values (default: `:$value`)
;;     - `:splitter` - string, regular expression or function which splits source column names into data
;;     - `:drop-missing?` - remove rows with missing? (default: `:true`)
;;     - `:datatypes` - map of target columns data types
;;
;; `:target-columns` - can be:
;;
;; * column name - source columns names are put there as a data
;; * column names as seqence - source columns names after split are put separately into `:target-columns` as data
;; * pattern - is a sequence of names, where some of the names are `nil`. `nil` is replaced by a name taken from splitter and such column is used for values.
;;
;; ---
;;
;; Create rows from all columns but `\"religion\"`.
;;


(def relig-income (tc/dataset "data/relig_income.csv"))


;;


relig-income


;;


(tc/pivot->longer relig-income (complement #{"religion"}))


;;
;; ---
;;
;; Convert only columns starting with `\"wk\"` and pack them into `:week` column, values go to `:rank` column
;;


(def bilboard (-> (tc/dataset "data/billboard.csv.gz")
                  (tc/drop-columns :type/boolean))) ;; drop some boolean columns, tidyr just skips them


;;


(->> bilboard
     (tc/column-names)
     (take 13)
     (tc/select-columns bilboard))


;;


(tc/pivot->longer bilboard #(clojure.string/starts-with? % "wk") {:target-columns :week
                                                                  :value-column-name :rank})


;;
;; ---
;;
;; We can create numerical column out of column names
;;


(tc/pivot->longer bilboard #(clojure.string/starts-with? % "wk") {:target-columns :week
                                                                  :value-column-name :rank
                                                                  :splitter #"wk(.*)"
                                                                  :datatypes {:week :int16}})



;;
;; ---
;;
;; When column names contain observation data, such column names can be splitted and data can be restored into separate columns.
;;


(def who (tc/dataset "data/who.csv.gz"))


;;


(->> who
     (tc/column-names)
     (take 10)
     (tc/select-columns who))


;;


(tc/pivot->longer who #(clojure.string/starts-with? % "new") {:target-columns [:diagnosis :gender :age]
                                                              :splitter #"new_?(.*)_(.)(.*)"
                                                              :value-column-name :count})


;;
;; ---
;;
;; When data contains multiple observations per row, we can use splitter and pattern for target columns to create new columns and put values there. In following dataset we have two obseravations `dob` and `gender` for two childs. We want to put child infomation into the column and leave dob and gender for values.
;;


(def family (tc/dataset "data/family.csv"))


;;


family


;;


(tc/pivot->longer family (complement #{"family"}) {:target-columns [nil :child]
                                                   :splitter "_"
                                                   :datatypes {"gender" :int16}})


;;
;; ---
;;
;; Similar here, we have two observations: `x` and `y` in four groups.
;;


(def anscombe (tc/dataset "data/anscombe.csv"))


;;


anscombe


;;


(tc/pivot->longer anscombe :all {:splitter #"(.)(.)"
                                 :target-columns [nil :set]})


;;
;; ---
;;


(def pnl (tc/dataset {:x [1 2 3 4]
                      :a [1 1 0 0]
                      :b [0 1 1 1]
                      :y1 (repeatedly 4 rand)
                      :y2 (repeatedly 4 rand)
                      :z1 [3 3 3 3]
                      :z2 [-2 -2 -2 -2]}))


;;


pnl


;;


(tc/pivot->longer pnl [:y1 :y2 :z1 :z2] {:target-columns [nil :times]
                                         :splitter #":(.)(.)"})


;;
;; #### Wider
;;
;; `pivot->wider` converts rows to columns.
;;
;; Arguments:
;;
;; * dataset
;; * `columns-selector` - values from selected columns are converted to new columns
;; * `value-columns` - what are values
;;
;; When multiple columns are used as columns selector, names are joined using `:concat-columns-with` option.
;; `:concat-columns-with` can be a string or function (default: \"_\"). Function accepts sequence of names.
;;
;; When `columns-selector` creates non unique set of values, they are folded using `:fold-fn` (default: `vec`) option.
;;
;; When `value-columns` is a sequence, multiple observations as columns are created appending value column names into new columns. Column names are joined using `:concat-value-with` option.
;; `:concat-value-with` can be a string or function (default: \"-\"). Function accepts current column name and value.
;;
;; ---
;;
;; Use `station` as a name source for columns and `seen` for values
;;


(def fish (tc/dataset "data/fish_encounters.csv"))


;;


fish


;;


(tc/pivot->wider fish "station" "seen" {:drop-missing? false})


;;
;; ---
;;
;; If selected columns contain multiple values, such values should be folded.
;;


(def warpbreaks (tc/dataset "data/warpbreaks.csv"))


;;


warpbreaks


;;
;; Let's see how many values are for each type of `wool` and `tension` groups
;;


(-> warpbreaks
    (tc/group-by ["wool" "tension"])
    (tc/aggregate {:n tc/row-count}))


;;


(-> warpbreaks
    (tc/reorder-columns ["wool" "tension" "breaks"])
    (tc/pivot->wider "wool" "breaks" {:fold-fn vec}))


;;
;; We can also calculate mean (aggreate values)
;;


(-> warpbreaks
    (tc/reorder-columns ["wool" "tension" "breaks"])
    (tc/pivot->wider "wool" "breaks" {:fold-fn tech.v3.datatype.functional/mean}))


;;
;; ---
;;
;; Multiple source columns, joined with default separator.
;;


(def production (tc/dataset "data/production.csv"))


;;


production


;;


(tc/pivot->wider production ["product" "country"] "production")


;;
;; Joined with custom function
;;


(tc/pivot->wider production ["product" "country"] "production" {:concat-columns-with vec})


;;
;; ---
;;
;; Multiple value columns
;;


(def income (tc/dataset "data/us_rent_income.csv"))


;;


income


;;


(tc/pivot->wider income "variable" ["estimate" "moe"] {:drop-missing? false})


;;
;; Value concatenated by custom function
;;


(tc/pivot->wider income "variable" ["estimate" "moe"] {:concat-columns-with vec
                                                       :concat-value-with vector
                                                       :drop-missing? false})


;;
;; ---
;;
;; Reshape contact data
;;


(def contacts (tc/dataset "data/contacts.csv"))


;;


contacts


;;


(tc/pivot->wider contacts "field" "value" {:drop-missing? false})



;;
;; #### Reshaping
;;
;; A couple of `tidyr` examples of more complex reshaping.
;;
;; ---
;;
;; [World bank](https://tidyr.tidyverse.org/articles/pivot.html#world-bank)
;;


(def world-bank-pop (tc/dataset "data/world_bank_pop.csv.gz"))


;;


(->> world-bank-pop
     (tc/column-names)
     (take 8)
     (tc/select-columns world-bank-pop))


;;
;; Step 1 - convert years column into values
;;


(def pop2 (tc/pivot->longer world-bank-pop (map str (range 2000 2018)) {:drop-missing? false
                                                                        :target-columns ["year"]
                                                                        :value-column-name "value"}))


;;


pop2


;;
;; Step 2 - separate `\"indicate\"` column
;;


(def pop3 (tc/separate-column pop2
                              "indicator" ["area" "variable"]
                              #(rest (clojure.string/split % #"\."))))


;;


pop3


;;
;; Step 3 - Make columns based on `\"variable\"` values.
;;


(tc/pivot->wider pop3 "variable" "value" {:drop-missing? false})


;;
;; ---
;;
;; ---
;;
;; [Multi-choice](https://tidyr.tidyverse.org/articles/pivot.html#multi-choice)
;;


(def multi (tc/dataset {:id [1 2 3 4]
                        :choice1 ["A" "C" "D" "B"]
                        :choice2 ["B" "B" nil "D"]
                        :choice3 ["C" nil nil nil]}))


;;


multi


;;
;; Step 1 - convert all choices into rows and add artificial column to all values which are not missing.
;;


(def multi2 (-> multi
                (tc/pivot->longer (complement #{:id}))
                (tc/add-column :checked true)))


;;


multi2


;;
;; Step 2 - Convert back to wide form with actual choices as columns
;;


(-> multi2
    (tc/drop-columns :$column)
    (tc/pivot->wider :$value :checked {:drop-missing? false})
    (tc/order-by :id))


;;
;; ---
;;
;; ---
;;
;; [Construction](https://tidyr.tidyverse.org/articles/pivot.html#by-hand)
;;


(def construction (tc/dataset "data/construction.csv"))
(def construction-unit-map {"1 unit" "1"
                            "2 to 4 units" "2-4"
                            "5 units or more" "5+"})



;;


construction


;;
;; Conversion 1 - Group two column types
;;


(-> construction
    (tc/pivot->longer #"^[125NWS].*|Midwest" {:target-columns [:units :region]
                                              :splitter (fn [col-name]
                                                          (if (re-matches #"^[125].*" col-name)
                                                            [(construction-unit-map col-name) nil]
                                                            [nil col-name]))
                                              :value-column-name :n
                                              :drop-missing? false}))


;;
;; Conversion 2 - Convert to longer form and back and rename columns
;;


(-> construction
    (tc/pivot->longer #"^[125NWS].*|Midwest" {:target-columns [:units :region]
                                              :splitter (fn [col-name]
                                                          (if (re-matches #"^[125].*" col-name)
                                                            [(construction-unit-map col-name) nil]
                                                            [nil col-name]))
                                              :value-column-name :n
                                              :drop-missing? false})
    (tc/pivot->wider [:units :region] :n {:drop-missing? false})
    (tc/rename-columns (zipmap (vals construction-unit-map)
                               (keys construction-unit-map))))


;;
;; ---
;;
;; Various operations on stocks, examples taken from [gather](https://tidyr.tidyverse.org/reference/gather.html) and [spread](https://tidyr.tidyverse.org/reference/spread.html) manuals.
;;


(def stocks-tidyr (tc/dataset "data/stockstidyr.csv"))


;;


stocks-tidyr


;;
;; Convert to longer form
;;


(def stocks-long (tc/pivot->longer stocks-tidyr ["X" "Y" "Z"] {:value-column-name :price
                                                               :target-columns :stocks}))


;;


stocks-long


;;
;; Convert back to wide form
;;


(tc/pivot->wider stocks-long :stocks :price)


;;
;; Convert to wide form on time column (let's limit values to a couple of rows)
;;


(-> stocks-long
    (tc/select-rows (range 0 30 4))
    (tc/pivot->wider "time" :price {:drop-missing? false}))


;;
;; ### Join/Concat Datasets
;;
;; Dataset join and concatenation functions.
;;
;; Joins accept left-side and right-side datasets and columns selector. Options are the same as in `tech.ml.dataset` functions.
;;
;; The difference between `tech.ml.dataset` join functions are: arguments order (first datasets) and possibility to join on multiple columns.
;;
;; Additionally set operations are defined: `intersect` and `difference`.
;;
;; To concat two datasets rowwise you can choose:
;;
;; * `concat` - concats rows for matching columns, the number of columns should be equal.
;; * `union` - like concat but returns unique values
;; * `bind` - concats rows add missing, empty columns
;;
;; To add two datasets columnwise use `bind`. The number of rows should be equal.
;;
;; Datasets used in examples:
;;


(def ds1 (tc/dataset {:a [1 2 1 2 3 4 nil nil 4]
                      :b (range 101 110)
                      :c (map str "abs tract")}))
(def ds2 (tc/dataset {:a [nil 1 2 5 4 3 2 1 nil]
                      :b (range 110 101 -1)
                      :c (map str "datatable")
                      :d (symbol "X")}))


;;


ds1
ds2


;;
;; #### Left
;;


(tc/left-join ds1 ds2 :b)


;;
;; ---
;;


(tc/left-join ds2 ds1 :b)


;;
;; ---
;;


(tc/left-join ds1 ds2 [:a :b])


;;
;; ---
;;


(tc/left-join ds2 ds1 [:a :b])


;;
;; #### Right
;;


(tc/right-join ds1 ds2 :b)


;;
;; ---
;;


(tc/right-join ds2 ds1 :b)


;;
;; ---
;;


(tc/right-join ds1 ds2 [:a :b])


;;
;; ---
;;


(tc/right-join ds2 ds1 [:a :b])


;;
;; #### Inner
;;


(tc/inner-join ds1 ds2 :b)


;;
;; ---
;;


(tc/inner-join ds2 ds1 :b)


;;
;; ---
;;


(tc/inner-join ds1 ds2 [:a :b])


;;
;; ---
;;


(tc/inner-join ds2 ds1 [:a :b])


;;
;; #### Full
;;
;; Join keeping all rows
;;


(tc/full-join ds1 ds2 :b)


;;
;; ---
;;


(tc/full-join ds2 ds1 :b)


;;
;; ---
;;


(tc/full-join ds1 ds2 [:a :b])


;;
;; ---
;;


(tc/full-join ds2 ds1 [:a :b])


;;
;; #### Semi
;;
;; Return rows from ds1 matching ds2
;;


(tc/semi-join ds1 ds2 :b)


;;
;; ---
;;


(tc/semi-join ds2 ds1 :b)


;;
;; ---
;;


(tc/semi-join ds1 ds2 [:a :b])


;;
;; ---
;;


(tc/semi-join ds2 ds1 [:a :b])


;;
;; #### Anti
;;
;; Return rows from ds1 not matching ds2
;;


(tc/anti-join ds1 ds2 :b)


;;
;; ---
;;


(tc/anti-join ds2 ds1 :b)


;;
;; ---
;;


(tc/anti-join ds1 ds2 [:a :b])


;;
;; ---
;;


(tc/anti-join ds2 ds1 [:a :b])


;; #### asof
;;


(def left-ds (tc/dataset {:a [1 5 10]
                          :left-val ["a" "b" "c"]}))
(def right-ds (tc/dataset {:a [1 2 3 6 7]
                           :right-val [:a :b :c :d :e]}))


;;


left-ds
right-ds


;;


(tc/asof-join left-ds right-ds :a)


;;


(tc/asof-join left-ds right-ds :a {:asof-op :nearest})


;;


(tc/asof-join left-ds right-ds :a {:asof-op :>=})


;;
;; #### Concat
;;
;; `contact` joins rows from other datasets
;;


(tc/concat ds1)


;;
;; ---
;;
;; `concat-copying` ensures all readers are evaluated.
;;


(tc/concat-copying ds1)


;;
;; ---
;;


(tc/concat ds1 (tc/drop-columns ds2 :d))


;;
;; ---
;;


(apply tc/concat (repeatedly 3 #(tc/random DS)))


;;
;; ##### Concat grouped dataset
;;
;; Concatenation of grouped datasets results also in grouped dataset.
;;


(tc/concat (tc/group-by DS [:V3])
           (tc/group-by DS [:V4]))


;;
;;
;; #### Union
;;
;; The same as `concat` but returns unique rows
;;


(apply tc/union (tc/drop-columns ds2 :d) (repeat 10 ds1))


;;
;; ---
;;


(apply tc/union (repeatedly 10 #(tc/random DS)))


;;
;; #### Bind
;;
;; `bind` adds empty columns during concat
;;


(tc/bind ds1 ds2)


;;
;; ---
;;


(tc/bind ds2 ds1)


;;
;; #### Append
;;
;; `append` concats columns
;;


(tc/append ds1 ds2)


;;
;; #### Intersection
;;


(tc/intersect (tc/select-columns ds1 :b)
              (tc/select-columns ds2 :b))


;;
;; #### Difference
;;


(tc/difference (tc/select-columns ds1 :b)
               (tc/select-columns ds2 :b))


;;
;; ---
;;


(tc/difference (tc/select-columns ds2 :b)
               (tc/select-columns ds1 :b))


;;
;; ### Split into train/test
;;
;; In ML world very often you need to test given model and prepare collection of train and test datasets. `split` creates new dataset with two additional columns:
;;
;; * `:$split-name` - with `:train`, `:test`, `:split-2`, ... values
;; * `:$split-id` - id of splitted group (for k-fold and repeating)
;;
;; `split-type` can be one of the following:
;;
;; * `:kfold` (default) - k-fold strategy, `:k` defines number of folds (defaults to `5`), produces `k` splits
;; * `:bootstrap` - `:ratio` defines ratio of observations put into result (defaults to `1.0`), produces `1` split
;; * `:holdout` - split into two or more parts with given ratio(s) (defaults to `2/3`), produces `1` split
;; * `:holdouts` - splits into two parts for ascending ratio. Range of rations is given by `steps` option
;; * `:loo` - leave one out, produces the same number of splits as number of observations
;;
;; `:holdout` can accept also probabilites or ratios and can split to more than 2 subdatasets
;;
;; Additionally you can provide:
;;
;; * `:seed` - for random number generator
;; * `:shuffle?` - turn on/off shuffle of the rows (default: `true`)
;; * `:repeats` - repeat procedure `:repeats` times
;; * `:partition-selector` - same as in `group-by` for stratified splitting to reflect dataset structure in splits.
;; * `:split-names` names of subdatasets different than default, ie. `[:train :test :split-2 ...]`
;; * `:split-col-name` - a column where name of split is stored, either `:train` or `:test` values (default: `:$split-name`)
;; * `:split-id-col-name` - a column where id of the train/test pair is stored (default: `:$split-id`)
;;
;; In case of grouped dataset each group is processed separately.
;;
;; See [more](https://www.mitpressjournals.org/doi/pdf/10.1162/EVCO_a_00069)
;;


(def for-splitting (tc/dataset (map-indexed (fn [id v] {:id id
                                                        :partition v
                                                        :group (rand-nth [:g1 :g2 :g3])})
                                            (concat (repeat 20 :a) (repeat 5 :b)))))


;;


for-splitting


;;
;; #### k-Fold
;;
;; Returns `k=5` maps
;;


(-> for-splitting
    (tc/split)
    (tc/head 30))


;;
;; Partition according to `:k` column to reflect it's distribution
;;


(-> for-splitting
    (tc/split :kfold {:partition-selector :partition})
    (tc/head 30))


;;
;; #### Bootstrap
;;


(tc/split for-splitting :bootstrap)


;;
;; with repeats, to get 100 splits
;;


(-> for-splitting
    (tc/split :bootstrap {:repeats 100})
    (:$split-id)
    (distinct)
    (count))


;;
;; #### Holdout
;;
;; with small ratio
;;


(tc/split for-splitting :holdout {:ratio 0.2})


;;
;; you can split to more than two subdatasets with holdout
;;


(tc/split for-splitting :holdout {:ratio [0.1 0.2 0.3 0.15 0.25]})


;;
;; you can use also proportions with custom names
;;


(tc/split for-splitting :holdout {:ratio [5 3 11 2]
                                  :split-names ["small" "smaller" "big" "the rest"]})


;;
;; #### Holdouts
;;
;; With ratios from 5% to 95% of the dataset with step 1.5 generates 15 splits with ascending rows in train dataset.
;;


(-> (tc/split for-splitting :holdouts {:steps [0.05 0.95 1.5]
                                       :shuffle? false})
    (tc/group-by [:$split-id :$split-name]))


;;
;;
;; #### Leave One Out
;;


(-> for-splitting
    (tc/split :loo)
    (tc/head 30))


;;


(-> for-splitting
    (tc/split :loo)
    (tc/row-count))


;;
;; #### Grouped dataset with partitioning
;;


(-> for-splitting
    (tc/group-by :group)
    (tc/split :bootstrap {:partition-selector :partition :seed 11 :ratio 0.8}))


;;
;; #### Split as a sequence
;;
;; To get a sequence of pairs, use `split->seq` function
;;


(-> for-splitting
    (tc/split->seq :kfold {:partition-selector :partition})
    (first))


;;


(-> for-splitting
    (tc/group-by :group)
    (tc/split->seq :bootstrap {:partition-selector :partition :seed 11 :ratio 0.8 :repeats 2})
    (first))


;;
;; ## Pipeline
;;
;; `tablecloth.pipeline` exports special versions of API which create functions operating only on dataset. This creates the possibility to chain operations and compose them easily.
;;
;; There are two ways to create pipelines:
;;
;; * functional, as a composition of functions
;; * declarative, separating task declarations and concrete parametrization.
;;
;; Pipeline operations are prepared to work with [metamorph](https://github.com/scicloj/metamorph) library. That means that result of the pipeline is wrapped into a map and dataset is stored under `:metamorph/data` key.
;;
;; > **Warning: Duplicated `metamorph` pipeline functions are removed from `tablecloth.pipeline` namespace.**
;;
;; ## Functions
;;
;; This API doesn't provide any statistical, numerical or date/time functions. Use below namespaces:
;;
;; | Namespace | functions |
;; |-----------|-----------|
;; | `tech.v3.datatype.functional` | primitive oprations, reducers, statistics |
;; | `tech.v3.datatype.datetime` | date/time converters and operations|
;;
;; ## Other examples
;;
;; ### Stocks
;;


(defonce stocks (tc/dataset "https://raw.githubusercontent.com/techascent/tech.ml.dataset/master/test/data/stocks.csv" {:key-fn keyword}))


;;


stocks


;;


(-> stocks
    (tc/group-by (fn [row]
                   {:symbol (:symbol row)
                    :year (tech.v3.datatype.datetime/long-temporal-field :years (:date row))}))
    (tc/aggregate #(tech.v3.datatype.functional/mean (% :price)))
    (tc/order-by [:symbol :year]))


;;


(-> stocks
    (tc/group-by (juxt :symbol #(tech.v3.datatype.datetime/long-temporal-field :years (% :date))))
    (tc/aggregate #(tech.v3.datatype.functional/mean (% :price)))
    (tc/rename-columns {:$group-name-0 :symbol
                        :$group-name-1 :year}))


;;
;; ### data.table
;;
;; Below you can find comparizon between functionality of `data.table` and Clojure dataset API. I leave it without comments, please refer original document explaining details:
;;
;; [Introduction to `data.table`](https://rdatatable.gitlab.io/data.table/articles/datatable-intro.html)
;;
;; R
;;
;; ```{r}
;; library(data.table)
;; library(knitr)
;;
;; flights <- fread(\"https://raw.githubusercontent.com/Rdatatable/data.table/master/vignettes/flights14.csv\")
;;
;; kable(head(flights))
;; ```
;;
;; ---
;;
;; Clojure
;;


(require '[tech.v3.datatype.functional :as dfn]
         '[tech.v3.datatype.argops :as aops]
         '[tech.v3.datatype :as dtype])

(defonce flights (tc/dataset "https://raw.githubusercontent.com/Rdatatable/data.table/master/vignettes/flights14.csv"))


;;


(tc/head flights 6)


;;
;; #### Basics
;;
;; ##### Shape of loaded data
;;
;; R
;;
;; ```{r}
;; dim(flights)
;; ```
;;
;; ---
;;
;; Clojure
;;


(tc/shape flights)


;;
;; ##### What is `data.table`?
;;
;; R
;;
;; ```{r}
;; DT = data.table(
;;   ID = c(\"b\",\"b\",\"b\",\"a\",\"a\",\"c\"),
;;   a = 1:6,
;;   b = 7:12,
;;   c = 13:18
;; )
;;
;; kable(DT)
;;
;; class(DT$ID)
;; ```
;;
;; ---
;;
;; Clojure
;;


(def DT (tc/dataset {:ID ["b" "b" "b" "a" "a" "c"]
                     :a (range 1 7)
                     :b (range 7 13)
                     :c (range 13 19)}))


;;


DT


;;


(-> :ID DT meta :datatype)


;;
;; ##### Get all the flights with “JFK” as the origin airport in the month of June.
;;
;; R
;;
;; ```{r}
;; ans <- flights[origin == \"JFK\" & month == 6L]
;; kable(head(ans))
;; ```
;;
;; ---
;;
;; Clojure
;;


(-> flights
    (tc/select-rows (fn [row] (and (= (get row "origin") "JFK")
                                   (= (get row "month") 6))))
    (tc/head 6))


;;
;; ##### Get the first two rows from `flights`.
;;
;; R
;;
;; ```{r}
;; ans <- flights[1:2]
;; kable(ans)
;; ```
;;
;; ---
;;
;; Clojure
;;


(tc/select-rows flights (range 2))


;;
;; ##### Sort `flights` first by column `origin` in ascending order, and then by `dest` in descending order
;;
;; R
;;
;; ```{r}
;; ans <- flights[order(origin, -dest)]
;; kable(head(ans))
;; ```
;;
;; ---
;;
;; Clojure
;;


(-> flights
    (tc/order-by ["origin" "dest"] [:asc :desc])
    (tc/head 6))


;;
;; ##### Select `arr_delay` column, but return it as a vector
;;
;; R
;;
;; ```{r}
;; ans <- flights[, arr_delay]
;; head(ans)
;; ```
;;
;; ---
;;
;; Clojure
;;


(take 6 (flights "arr_delay"))


;;
;; ##### Select `arr_delay` column, but return as a data.table instead
;;
;; R
;;
;; ```{r}
;; ans <- flights[, list(arr_delay)]
;; kable(head(ans))
;; ```
;;
;; ---
;;
;; Clojure
;;


(-> flights
    (tc/select-columns "arr_delay")
    (tc/head 6))


;;
;; ##### Select both `arr_delay` and `dep_delay` columns
;;
;; R
;;
;; ```{r}
;; ans <- flights[, .(arr_delay, dep_delay)]
;; kable(head(ans))
;; ```
;;
;; ---
;;
;; Clojure
;;


(-> flights
    (tc/select-columns ["arr_delay" "dep_delay"])
    (tc/head 6))


;;
;; ##### Select both `arr_delay` and `dep_delay` columns and rename them to `delay_arr` and `delay_dep`
;;
;; R
;;
;; ```{r}
;; ans <- flights[, .(delay_arr = arr_delay, delay_dep = dep_delay)]
;; kable(head(ans))
;; ```
;;
;; ---
;;
;; Clojure
;;


(-> flights
    (tc/select-columns {"arr_delay" "delay_arr"
                        "dep_delay" "delay_arr"})
    (tc/head 6))


;;
;; ##### How many trips have had total delay < 0?
;;
;; R
;;
;; ```{r}
;; ans <- flights[, sum( (arr_delay + dep_delay) < 0 )]
;; ans
;; ```
;;
;; ---
;;
;; Clojure
;;


(->> (dfn/+ (flights "arr_delay") (flights "dep_delay"))
     (aops/argfilter #(< % 0.0))
     (dtype/ecount))


;;
;; or pure Clojure functions (much, much slower)
;;


(->> (map + (flights "arr_delay") (flights "dep_delay"))
     (filter neg?)
     (count))


;;
;; ##### Calculate the average arrival and departure delay for all flights with “JFK” as the origin airport in the month of June
;;
;; R
;;
;; ```{r}
;; ans <- flights[origin == \"JFK\" & month == 6L,
;;                .(m_arr = mean(arr_delay), m_dep = mean(dep_delay))]
;; kable(ans)
;; ```
;;
;; ---
;;
;; Clojure
;;


(-> flights
    (tc/select-rows (fn [row] (and (= (get row "origin") "JFK")
                                   (= (get row "month") 6))))
    (tc/aggregate {:m_arr #(dfn/mean (% "arr_delay"))
                   :m_dep #(dfn/mean (% "dep_delay"))}))


;;
;; ##### How many trips have been made in 2014 from “JFK” airport in the month of June?
;;
;; R
;;
;; ```{r}
;; ans <- flights[origin == \"JFK\" & month == 6L, length(dest)]
;; ans
;; ```
;;
;; or
;;
;; ```{r}
;; ans <- flights[origin == \"JFK\" & month == 6L, .N]
;; ans
;; ```
;;
;; ---
;;
;; Clojure
;;


(-> flights
    (tc/select-rows (fn [row] (and (= (get row "origin") "JFK")
                                   (= (get row "month") 6))))
    (tc/row-count))


;;
;; ##### deselect columns using - or !
;;
;; R
;;
;; ```{r}
;; ans <- flights[, !c(\"arr_delay\", \"dep_delay\")]
;; kable(head(ans))
;; ```
;;
;; or
;;
;; ```{r}
;; ans <- flights[, -c(\"arr_delay\", \"dep_delay\")]
;; kable(head(ans))
;; ```
;;
;; ---
;;
;; Clojure
;;


(-> flights
    (tc/select-columns (complement #{"arr_delay" "dep_delay"}))
    (tc/head 6))


;;
;; #### Aggregations
;;
;; ##### How can we get the number of trips corresponding to each origin airport?
;;
;; R
;;
;; ```{r}
;; ans <- flights[, .(.N), by = .(origin)]
;; kable(ans)
;; ```
;;
;; ---
;;
;; Clojure
;;


(-> flights
    (tc/group-by ["origin"])
    (tc/aggregate {:N tc/row-count}))


;;
;; ##### How can we calculate the number of trips for each origin airport for carrier code \"AA\"?
;;
;; R
;;
;; ```{r}
;; ans <- flights[carrier == \"AA\", .N, by = origin]
;; kable(ans)
;; ```
;;
;; ---
;;
;; Clojure
;;


(-> flights
    (tc/select-rows #(= (get % "carrier") "AA"))
    (tc/group-by ["origin"])
    (tc/aggregate {:N tc/row-count}))


;;
;; ##### How can we get the total number of trips for each `origin`, `dest` pair for carrier code \"AA\"?
;;
;; R
;;
;; ```{r}
;; ans <- flights[carrier == \"AA\", .N, by = .(origin, dest)]
;; kable(head(ans))
;; ```
;;
;; ---
;;
;; Clojure
;;


(-> flights
    (tc/select-rows #(= (get % "carrier") "AA"))
    (tc/group-by ["origin" "dest"])
    (tc/aggregate {:N tc/row-count})
    (tc/head 6))


;;
;; ##### How can we get the average arrival and departure delay for each `orig`,`dest` pair for each month for carrier code \"AA\"?
;;
;; R
;;
;; ```{r}
;; ans <- flights[carrier == \"AA\",
;;         .(mean(arr_delay), mean(dep_delay)),
;;         by = .(origin, dest, month)]
;; kable(head(ans,10))
;; ```
;;
;; ---
;;
;; Clojure
;;


(-> flights
    (tc/select-rows #(= (get % "carrier") "AA"))
    (tc/group-by ["origin" "dest" "month"])
    (tc/aggregate [#(dfn/mean (% "arr_delay"))
                   #(dfn/mean (% "dep_delay"))])
    (tc/head 10))


;;
;; ##### So how can we directly order by all the grouping variables?
;;
;; R
;;
;; ```{r}
;; ans <- flights[carrier == \"AA\",
;;         .(mean(arr_delay), mean(dep_delay)),
;;         keyby = .(origin, dest, month)]
;; kable(head(ans,10))
;; ```
;;
;; ---
;;
;; Clojure
;;


(-> flights
    (tc/select-rows #(= (get % "carrier") "AA"))
    (tc/group-by ["origin" "dest" "month"])
    (tc/aggregate [#(dfn/mean (% "arr_delay"))
                   #(dfn/mean (% "dep_delay"))])
    (tc/order-by ["origin" "dest" "month"])
    (tc/head 10))


;;
;; ##### Can `by` accept expressions as well or does it just take columns?
;;
;; R
;;
;; ```{r}
;; ans <- flights[, .N, .(dep_delay>0, arr_delay>0)]
;; kable(ans)
;; ```
;;
;; ---
;;
;; Clojure
;;


(-> flights
    (tc/group-by (fn [row]
                   {:dep_delay (pos? (get row "dep_delay"))
                    :arr_delay (pos? (get row "arr_delay"))}))
    (tc/aggregate {:N tc/row-count}))


;;
;; ##### Do we have to compute `mean()` for each column individually?
;;
;; R
;;
;; ```{r}
;; kable(DT)
;;
;; DT[, print(.SD), by = ID]
;; ```
;;
;; ```{r}
;; kable(DT[, lapply(.SD, mean), by = ID])
;; ```
;;
;; ---
;;
;; Clojure
;;


DT

(tc/group-by DT :ID {:result-type :as-map})


;;


(-> DT
    (tc/group-by [:ID])
    (tc/aggregate-columns (complement #{:ID}) dfn/mean))


;;
;; ##### How can we specify just the columns we would like to compute the `mean()` on?
;;
;; R
;;
;; ```{r}
;; kable(head(flights[carrier == \"AA\",                         ## Only on trips with carrier \"AA\"
;;                    lapply(.SD, mean),                       ## compute the mean
;;                    by = .(origin, dest, month),             ## for every 'origin,dest,month'
;;                    .SDcols = c(\"arr_delay\", \"dep_delay\")])) ## for just those specified in .SDcols
;; ```
;;
;; ---
;;
;; Clojure
;;


(-> flights
    (tc/select-rows #(= (get % "carrier") "AA"))
    (tc/group-by ["origin" "dest" "month"])
    (tc/aggregate-columns ["arr_delay" "dep_delay"] dfn/mean)
    (tc/head 6))


;;
;; ##### How can we return the first two rows for each month?
;;
;; R
;;
;; ```{r}
;; ans <- flights[, head(.SD, 2), by = month]
;; kable(head(ans))
;; ```
;;
;; ---
;;
;; Clojure
;;


(-> flights
    (tc/group-by ["month"])
    (tc/head 2) ;; head applied on each group
    (tc/ungroup)
    (tc/head 6))


;;
;; ##### How can we concatenate columns a and b for each group in ID?
;;
;; R
;;
;; ```{r}
;; kable(DT[, .(val = c(a,b)), by = ID])
;; ```
;;
;; ---
;;
;; Clojure
;;


(-> DT
    (tc/pivot->longer [:a :b] {:value-column-name :val})
    (tc/drop-columns [:$column :c]))


;;
;; ##### What if we would like to have all the values of column `a` and `b` concatenated, but returned as a list column?
;;
;; R
;;
;; ```{r}
;; kable(DT[, .(val = list(c(a,b))), by = ID])
;; ```
;;
;; ---
;;
;; Clojure
;;


(-> DT
    (tc/pivot->longer [:a :b] {:value-column-name :val})
    (tc/drop-columns [:$column :c])
    (tc/fold-by :ID))


;;
;; ### API tour
;;
;; Below snippets are taken from [A data.table and dplyr tour](https://atrebas.github.io/post/2019-03-03-datatable-dplyr/) written by Atrebas (permission granted).
;;
;; I keep structure and subtitles but I skip `data.table` and `dplyr` examples.
;;
;; Example data
;;


(def DS (tc/dataset {:V1 (take 9 (cycle [1 2]))
                     :V2 (range 1 10)
                     :V3 (take 9 (cycle [0.5 1.0 1.5]))
                     :V4 (take 9 (cycle ["A" "B" "C"]))}))


;;


(tc/dataset? DS)
(class DS)


;;


DS


;;
;; #### Basic Operations
;;
;; ##### Filter rows
;;
;; Filter rows using indices
;;


(tc/select-rows DS [2 3])


;;
;; ---
;;
;; Discard rows using negative indices
;;
;; In Clojure API we have separate function for that: `drop-rows`.
;;


(tc/drop-rows DS (range 2 7))


;;
;; ---
;;
;; Filter rows using a logical expression
;;


(tc/select-rows DS (comp #(> % 5) :V2))


;;


(tc/select-rows DS (comp #{"A" "C"} :V4))


;;
;; ---
;;
;; Filter rows using multiple conditions
;;


(tc/select-rows DS #(and (= (:V1 %) 1)
                         (= (:V4 %) "A")))


;;
;; ---
;;
;; Filter unique rows
;;


(tc/unique-by DS)


;;


(tc/unique-by DS [:V1 :V4])


;;
;; ---
;;
;; Discard rows with missing values
;;


(tc/drop-missing DS)


;;
;; ---
;;
;; Other filters
;;


(tc/random DS 3) ;; 3 random rows


;;


(tc/random DS (/ (tc/row-count DS) 2)) ;; fraction of random rows


;;


(tc/by-rank DS :V1 zero?) ;; take top n entries


;;
;; ---
;;
;; Convenience functions
;;


(tc/select-rows DS (comp (partial re-matches #"^B") str :V4))


;;


(tc/select-rows DS (comp #(<= 3 % 5) :V2))


;;


(tc/select-rows DS (comp #(< 3 % 5) :V2))


;;


(tc/select-rows DS (comp #(<= 3 % 5) :V2))


;;
;; Last example skipped.
;;
;; ##### Sort rows
;;
;; Sort rows by column
;;


(tc/order-by DS :V3)


;;
;; ---
;;
;; Sort rows in decreasing order
;;


(tc/order-by DS :V3 :desc)


;;
;; ---
;;
;; Sort rows based on several columns
;;


(tc/order-by DS [:V1 :V2] [:asc :desc])


;;
;; ##### Select columns
;;
;; Select one column using an index (not recommended)
;;


(nth (tc/columns DS :as-seq) 2) ;; as column (iterable)


;;


(tc/dataset [(nth (tc/columns DS :as-seq) 2)])


;;
;; ---
;;
;; Select one column using column name
;;


(tc/select-columns DS :V2) ;; as dataset


;;


(tc/select-columns DS [:V2]) ;; as dataset


;;


(DS :V2) ;; as column (iterable)


;;
;; ---
;;
;; Select several columns
;;


(tc/select-columns DS [:V2 :V3 :V4])


;;
;; ---
;;
;; Exclude columns
;;


(tc/select-columns DS (complement #{:V2 :V3 :V4}))


;;


(tc/drop-columns DS [:V2 :V3 :V4])


;;
;; ---
;;
;; Other seletions
;;


(->> (range 1 3)
     (map (comp keyword (partial format "V%d")))
     (tc/select-columns DS))


;;


(tc/reorder-columns DS :V4)


;;


(tc/select-columns DS #(clojure.string/starts-with? (name %) "V"))


;;


(tc/select-columns DS #(clojure.string/ends-with? (name %) "3"))


;;


(tc/select-columns DS #"..2") ;; regex converts to string using `str` function


;;


(tc/select-columns DS #{:V1 "X"})


;;


(tc/select-columns DS #(not (clojure.string/starts-with? (name %) "V2")))


;;
;; ##### Summarise data
;;
;; Summarise one column
;;


(reduce + (DS :V1)) ;; using pure Clojure, as value


;;


(tc/aggregate-columns DS :V1 dfn/sum) ;; as dataset


;;


(tc/aggregate DS {:sumV1 #(dfn/sum (% :V1))})


;;
;; ---
;;
;; Summarize several columns
;;


(tc/aggregate DS [#(dfn/sum (% :V1))
                  #(dfn/standard-deviation (% :V3))])


;;


(tc/aggregate-columns DS [:V1 :V3] [dfn/sum
                                    dfn/standard-deviation])


;;
;;
;; ---
;;
;; Summarise several columns and assign column names
;;


(tc/aggregate DS {:sumv1 #(dfn/sum (% :V1))
                  :sdv3 #(dfn/standard-deviation (% :V3))})


;;
;; ---
;;
;; Summarise a subset of rows
;;


(-> DS
    (tc/select-rows (range 4))
    (tc/aggregate-columns :V1 dfn/sum))


;;
;; ##### Additional helpers
;;
;;


(-> DS
    (tc/first)
    (tc/select-columns :V3)) ;; select first row from `:V3` column


;;


(-> DS
    (tc/last)
    (tc/select-columns :V3)) ;; select last row from `:V3` column


;;


(-> DS
    (tc/select-rows 4)
    (tc/select-columns :V3)) ;; select forth row from `:V3` column


;;


(-> DS
    (tc/select :V3 4)) ;; select forth row from `:V3` column


;;


(-> DS
    (tc/unique-by :V4)
    (tc/aggregate tc/row-count)) ;; number of unique rows in `:V4` column, as dataset


;;
;;


(-> DS
    (tc/unique-by :V4)
    (tc/row-count)) ;; number of unique rows in `:V4` column, as value


;;


(-> DS
    (tc/unique-by)
    (tc/row-count)) ;; number of unique rows in dataset, as value


;;
;; ##### Add/update/delete columns
;;
;; Modify a column
;;


(tc/map-columns DS :V1 [:V1] #(dfn/pow % 2))


;;


(def DS (tc/add-column DS :V1 (dfn/pow (DS :V1) 2)))


;;


DS


;;
;; ---
;;
;; Add one column
;;


(tc/map-columns DS :v5 [:V1] dfn/log)


;;


(def DS (tc/add-column DS :v5 (dfn/log (DS :V1))))


;;


DS


;;
;; ---
;;
;; Add several columns
;;


(def DS (tc/add-columns DS {:v6 (dfn/sqrt (DS :V1))
                            :v7 "X"}))


;;


DS


;;
;; ---
;;
;; Create one column and remove the others
;;


(tc/dataset {:v8 (dfn/+ (DS :V3) 1)})


;;
;; ---
;;
;; Remove one column
;;


(def DS (tc/drop-columns DS :v5))


;;


DS


;;
;; ---
;;
;; Remove several columns
;;


(def DS (tc/drop-columns DS [:v6 :v7]))


;;


DS


;;
;; ---
;;
;; Remove columns using a vector of colnames
;;
;; We use set here.
;;


(def DS (tc/select-columns DS (complement #{:V3})))


;;


DS


;;
;; ---
;;
;; Replace values for rows matching a condition
;;


(def DS (tc/map-columns DS :V2 [:V2] #(if (< % 4.0) 0.0 %)))


;;


DS


;;
;; ##### by
;;
;; By group
;;


(-> DS
    (tc/group-by [:V4])
    (tc/aggregate {:sumV2 #(dfn/sum (% :V2))}))


;;
;; ---
;;
;; By several groups
;;


(-> DS
    (tc/group-by [:V4 :V1])
    (tc/aggregate {:sumV2 #(dfn/sum (% :V2))}))


;;
;; ---
;;
;; Calling function in by
;;


(-> DS
    (tc/group-by (fn [row]
                   (clojure.string/lower-case (:V4 row))))
    (tc/aggregate {:sumV1 #(dfn/sum (% :V1))}))


;;
;; ---
;;
;; Assigning column name in by
;;


(-> DS
    (tc/group-by (fn [row]
                   {:abc (clojure.string/lower-case (:V4 row))}))
    (tc/aggregate {:sumV1 #(dfn/sum (% :V1))}))


;;


(-> DS
    (tc/group-by (fn [row]
                   (clojure.string/lower-case (:V4 row))))
    (tc/aggregate {:sumV1 #(dfn/sum (% :V1))} {:add-group-as-column :abc}))


;;
;; ---
;;
;; Using a condition in by
;;


(-> DS
    (tc/group-by #(= (:V4 %) "A"))
    (tc/aggregate #(dfn/sum (% :V1))))


;;
;; ---
;;
;; By on a subset of rows
;;


(-> DS
    (tc/select-rows (range 5))
    (tc/group-by :V4)
    (tc/aggregate {:sumV1 #(dfn/sum (% :V1))}))


;;
;; ---
;;
;; Count number of observations for each group
;;


(-> DS
    (tc/group-by :V4)
    (tc/aggregate tc/row-count))


;;
;; ---
;;
;; Add a column with number of observations for each group
;;


(-> DS
    (tc/group-by [:V1])
    (tc/add-column :n tc/row-count)
    (tc/ungroup))


;;
;; ---
;;
;; Retrieve the first/last/nth observation for each group
;;


(-> DS
    (tc/group-by [:V4])
    (tc/aggregate-columns :V2 first))


;;


(-> DS
    (tc/group-by [:V4])
    (tc/aggregate-columns :V2 last))


;;


(-> DS
    (tc/group-by [:V4])
    (tc/aggregate-columns :V2 #(nth % 1)))


;;
;; #### Going further
;;
;; ##### Advanced columns manipulation
;;
;; Summarise all the columns
;;


;; custom max function which works on every type
(tc/aggregate-columns DS :all (fn [col] (first (sort #(compare %2 %1) col))))


;;
;; ---
;;
;; Summarise several columns
;;


(tc/aggregate-columns DS [:V1 :V2] dfn/mean)


;;
;; ---
;;
;; Summarise several columns by group
;;


(-> DS
    (tc/group-by [:V4])
    (tc/aggregate-columns [:V1 :V2] dfn/mean))


;;
;; ---
;;
;; Summarise with more than one function by group
;;


(-> DS
    (tc/group-by [:V4])
    (tc/aggregate-columns [:V1 :V2] (fn [col]
                                      {:sum (dfn/sum col)
                                       :mean (dfn/mean col)})))


;;
;; Summarise using a condition
;;


(-> DS
    (tc/select-columns :type/numerical)
    (tc/aggregate-columns :all dfn/mean))


;;
;; ---
;;
;; Modify all the columns
;;


(tc/update-columns DS :all reverse)


;;
;; ---
;;
;; Modify several columns (dropping the others)
;;


(-> DS
    (tc/select-columns [:V1 :V2])
    (tc/update-columns :all dfn/sqrt))


;;


(-> DS
    (tc/select-columns (complement #{:V4}))
    (tc/update-columns :all dfn/exp))


;;
;; ---
;;
;; Modify several columns (keeping the others)
;;


(def DS (tc/update-columns DS [:V1 :V2] dfn/sqrt))


;;


DS


;;


(def DS (tc/update-columns DS (complement #{:V4}) #(dfn/pow % 2)))


;;


DS


;;
;; ---
;;
;; Modify columns using a condition (dropping the others)
;;


(-> DS
    (tc/select-columns :type/numerical)
    (tc/update-columns :all #(dfn/- % 1)))


;;
;; ---
;;
;; Modify columns using a condition (keeping the others)
;;


(def DS (tc/convert-types DS :type/numerical :int32))


;;


DS


;;
;; ---
;;
;; Use a complex expression
;;


(-> DS
    (tc/group-by [:V4])
    (tc/head 2)
    (tc/add-column :V2 "X")
    (tc/ungroup))


;;
;; ---
;;
;; Use multiple expressions
;;


(let [x (dfn/+ (DS :V1) (dfn/sum (DS :V2)))]
  [(clay/capture-print
    (println (seq (DS :V1))))
   (clay/capture-print
    (println (tc/info (tc/select-columns DS :V1))))
   (tc/dataset
    {:A (range 1 (inc (tc/row-count DS)))
     :B x})])


;;
;; ##### Chain expressions
;;
;; Expression chaining using >
;;


(-> DS
    (tc/group-by [:V4])
    (tc/aggregate {:V1sum #(dfn/sum (% :V1))})
    (tc/select-rows #(>= (:V1sum %) 5)))


;;


(-> DS
    (tc/group-by [:V4])
    (tc/aggregate {:V1sum #(dfn/sum (% :V1))})
    (tc/order-by :V1sum :desc))


;;
;; ##### Indexing and Keys
;;
;; Set the key/index (order)
;;


(def DS (tc/order-by DS :V4))


;;


DS


;;
;; Select the matching rows
;;


(tc/select-rows DS #(= (:V4 %) "A"))


;;


(tc/select-rows DS (comp #{"A" "C"} :V4))


;;
;; ---
;;
;; Select the first matching row
;;


(-> DS
    (tc/select-rows #(= (:V4 %) "B"))
    (tc/first))


;;


(-> DS
    (tc/unique-by :V4)
    (tc/select-rows (comp #{"B" "C"} :V4)))


;;
;; ---
;;
;; Select the last matching row
;;


(-> DS
    (tc/select-rows #(= (:V4 %) "A"))
    (tc/last))


;;
;; ---
;;
;; Nomatch argument
;;


(tc/select-rows DS (comp #{"A" "D"} :V4))


;;
;; ---
;;
;; Apply a function on the matching rows
;;


(-> DS
    (tc/select-rows (comp #{"A" "C"} :V4))
    (tc/aggregate-columns :V1 (fn [col]
                                {:sum (dfn/sum col)})))


;;
;; ---
;;
;; Modify values for matching rows
;;


(def DS (-> DS
            (tc/map-columns :V1 [:V1 :V4] #(if (= %2 "A") 0 %1))
            (tc/order-by :V4)))


;;


DS


;;
;; ---
;;
;; Use keys in by
;;


(-> DS
    (tc/select-rows (comp (complement #{"B"}) :V4))
    (tc/group-by [:V4])
    (tc/aggregate-columns :V1 dfn/sum))


;;
;; ---
;;
;; Set keys/indices for multiple columns (ordered)
;;


(tc/order-by DS [:V4 :V1])


;;
;; ---
;;
;; Subset using multiple keys/indices
;;


(-> DS
    (tc/select-rows #(and (= (:V1 %) 1)
                          (= (:V4 %) "C"))))


;;


(-> DS
    (tc/select-rows #(and (= (:V1 %) 1)
                          (#{"B" "C"} (:V4 %)))))


;;


(-> DS
    (tc/select-rows #(and (= (:V1 %) 1)
                          (#{"B" "C"} (:V4 %))) {:result-type :as-indexes}))


;;
;; ##### set*() modifications
;;
;; Replace values
;;
;; There is no mutating operations `tech.ml.dataset` or easy way to set value.
;;


(def DS (tc/update-columns DS :V2 #(map-indexed (fn [idx v]
                                                  (if (zero? idx) 3 v)) %)))


;;


DS


;;
;; ---
;;
;; Reorder rows
;;


(def DS (tc/order-by DS [:V4 :V1] [:asc :desc]))


;;


DS


;;
;; ---
;;
;; Modify colnames
;;


(def DS (tc/rename-columns DS {:V2 "v2"}))


;;


DS


;;


(def DS (tc/rename-columns DS {"v2" :V2})) ;; revert back


;;
;; ---
;;
;; Reorder columns
;;


(def DS (tc/reorder-columns DS :V4 :V1 :V2))


;;


DS


;;
;; ##### Advanced use of by
;;
;; Select first/last/… row by group
;;


(-> DS
    (tc/group-by :V4)
    (tc/first)
    (tc/ungroup))


;;


(-> DS
    (tc/group-by :V4)
    (tc/select-rows [0 2])
    (tc/ungroup))


;;


(-> DS
    (tc/group-by :V4)
    (tc/tail 2)
    (tc/ungroup))


;;
;; ---
;;
;; Select rows using a nested query
;;


(-> DS
    (tc/group-by :V4)
    (tc/order-by :V2)
    (tc/first)
    (tc/ungroup))


;;
;; Add a group counter column
;;


(-> DS
    (tc/group-by [:V4 :V1])
    (tc/ungroup {:add-group-id-as-column :Grp}))


;;
;; ---
;;
;; Get row number of first (and last) observation by group
;;


(-> DS
    (tc/add-column :row-id (range))
    (tc/select-columns [:V4 :row-id])
    (tc/group-by :V4)
    (tc/ungroup))


;;


(-> DS
    (tc/add-column :row-id (range))
    (tc/select-columns [:V4 :row-id])
    (tc/group-by :V4)
    (tc/first)
    (tc/ungroup))


;;


(-> DS
    (tc/add-column :row-id (range))
    (tc/select-columns [:V4 :row-id])
    (tc/group-by :V4)
    (tc/select-rows [0 2])
    (tc/ungroup))


;;
;; ---
;;
;; Handle list-columns by group
;;


(-> DS
    (tc/select-columns [:V1 :V4])
    (tc/fold-by :V4))


;;


(-> DS
    (tc/group-by :V4)
    (tc/unmark-group))


;;
;; ---
;;
;; Grouping sets (multiple by at once)
;;
;; Not available.
;;
;; #### Miscellaneous
;;
;; ##### Read / Write data
;;
;; Write data to a csv file
;;


(tc/write! DS "DF.csv")


;;
;; ---
;;
;; Write data to a tab-delimited file
;;


(tc/write! DS "DF.txt" {:separator \tab})


;;
;; or
;;


(tc/write! DS "DF.tsv")


;;
;; ---
;;
;; Read a csv / tab-delimited file
;;


(tc/dataset "DF.csv" {:key-fn keyword})


;;


(tc/dataset "DF.txt" {:key-fn keyword})


;;


(tc/dataset "DF.tsv" {:key-fn keyword})


;;
;; ---
;;
;; Read a csv file selecting / droping columns
;;


(tc/dataset "DF.csv" {:key-fn keyword
                      :column-whitelist ["V1" "V4"]})


;;


(tc/dataset "DF.csv" {:key-fn keyword
                      :column-blacklist ["V4"]})


;;
;; ---
;;
;; Read and rbind several files
;;


(apply tc/concat (map tc/dataset ["DF.csv" "DF.csv"]))


;;
;; ##### Reshape data
;;
;; Melt data (from wide to long)
;;


(def mDS (tc/pivot->longer DS [:V1 :V2] {:target-columns :variable
                                         :value-column-name :value}))


;;


mDS


;;
;; ---
;;
;; Cast data (from long to wide)
;;


(-> mDS
    (tc/pivot->wider :variable :value {:fold-fn vec})
    (tc/update-columns ["V1" "V2"] (partial map count)))


;;


(-> mDS
    (tc/pivot->wider :variable :value {:fold-fn vec})
    (tc/update-columns ["V1" "V2"] (partial map dfn/sum)))


;;


(-> mDS
    (tc/map-columns :value #(> % 5))
    (tc/pivot->wider :value :variable {:fold-fn vec})
    (tc/update-columns ["true" "false"] (partial map #(if (sequential? %) (count %) 1))))


;;
;; ---
;;
;; Split
;;


(tc/group-by DS :V4 {:result-type :as-map})


;;
;; ---
;;
;; Split and transpose a vector/column
;;


(-> {:a ["A:a" "B:b" "C:c"]}
    (tc/dataset)
    (tc/separate-column :a [:V1 :V2] ":"))


;;
;; ##### Other
;;
;; Skipped
;;
;; #### Join/Bind data sets
;;


(def x (tc/dataset {"Id" ["A" "B" "C" "C"]
                    "X1" [1 3 5 7]
                    "XY" ["x2" "x4" "x6" "x8"]}))
(def y (tc/dataset {"Id" ["A" "B" "B" "D"]
                    "Y1" [1 3 5 7]
                    "XY" ["y1" "y3" "y5" "y7"]}))


;;


x y


;;
;; ##### Join
;;
;; Join matching rows from y to x
;;


(tc/left-join x y "Id")


;;
;; ---
;;
;; Join matching rows from x to y
;;


(tc/right-join x y "Id")


;;
;; ---
;;
;; Join matching rows from both x and y
;;


(tc/inner-join x y "Id")


;;
;; ---
;;
;; Join keeping all the rows
;;


(tc/full-join x y "Id")


;;
;; ---
;;
;; Return rows from x matching y
;;


(tc/semi-join x y "Id")


;;
;; ---
;;
;; Return rows from x not matching y
;;


(tc/anti-join x y "Id")


;;
;; ##### More joins
;;
;; Select columns while joining
;;


(tc/right-join (tc/select-columns x ["Id" "X1"])
               (tc/select-columns y ["Id" "XY"])
               "Id")


;;


(tc/right-join (tc/select-columns x ["Id" "XY"])
               (tc/select-columns y ["Id" "XY"])
               "Id")


;;
;; Aggregate columns while joining
;;


(-> y
    (tc/group-by ["Id"])
    (tc/aggregate {"sumY1" #(dfn/sum (% "Y1"))})
    (tc/right-join x "Id")
    (tc/add-column "X1Y1" (fn [ds] (dfn/* (ds "sumY1")
                                          (ds "X1"))))
    (tc/select-columns ["right.Id" "X1Y1"]))


;;
;; Update columns while joining
;;


(-> x
    (tc/select-columns ["Id" "X1"])
    (tc/map-columns "SqX1" "X1" (fn [x] (* x x)))
    (tc/right-join y "Id")
    (tc/drop-columns ["X1" "Id"]))


;;
;; ---
;;
;; Adds a list column with rows from y matching x (nest-join)
;;
;;


(-> (tc/left-join x y "Id")
    (tc/drop-columns ["right.Id"])
    (tc/fold-by (tc/column-names x)))


;;
;; ---
;;
;; Some joins are skipped
;;
;; ---
;;
;; Cross join
;;


(def cjds (tc/dataset {:V1 [[2 1 1]]
                       :V2 [[3 2]]}))


;;


cjds


;;


(reduce #(tc/unroll %1 %2) cjds (tc/column-names cjds))


;;


(-> (reduce #(tc/unroll %1 %2) cjds (tc/column-names cjds))
    (tc/unique-by))


;;
;; ##### Bind
;;


(def x (tc/dataset {:V1 [1 2 3]}))
(def y (tc/dataset {:V1 [4 5 6]}))
(def z (tc/dataset {:V1 [7 8 9]
                    :V2 [0 0 0]}))


;;


x y z


;;
;; ---
;;
;; Bind rows
;;


(tc/bind x y)


;;


(tc/bind x z)


;;
;; ---
;;
;; Bind rows using a list
;;


(->> [x y]
     (map-indexed #(tc/add-column %2 :id (repeat %1)))
     (apply tc/bind))


;;
;; ---
;;
;; Bind columns
;;


(tc/append x y)


;;
;; ##### Set operations
;;


(def x (tc/dataset {:V1 [1 2 2 3 3]}))
(def y (tc/dataset {:V1 [2 2 3 4 4]}))


;;


x y


;;
;; ---
;;
;; Intersection
;;


(tc/intersect x y)


;;
;; ---
;;
;; Difference
;;


(tc/difference x y)


;;
;; ---
;;
;; Union
;;


(tc/union x y)


;;


(tc/concat x y)


;;
;; ---
;;
;; Equality not implemented
