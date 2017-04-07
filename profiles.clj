;; These aren't sourced yet, the project.clj would need to merge the relevant
;; top-level keys, for example in {:profiles {:dev [:env/dev {...}]}}
{:env/dev  {:env {}}
 :env/ci   {:env {}}
 :env/test {:env {}}}
