(ns baseet.lifecycle)

(defprotocol Lifecycle
  "A protocol specifying the lifecycle protocol per S.Siera"
  (start [_] "Starting service")
  (stop [_] "Stopping service"))


