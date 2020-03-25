#!/bin/sh
base_dir=$(dirname "$0")
base_dir=$(
  cd "$base_dir" || exit
  pwd
)
java -cp .:conf:lib/* org.hy.hbase.AppMain $1 $2 $3 $4 $5 $6 $7 $8 $9 -window
