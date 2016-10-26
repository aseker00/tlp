#!/bin/bash
seq 0 499 | xargs -i bash -c 'sed -n $(( {} * 2 + 1 )),$(( {} * 2 + 2 ))p tmp | sed "s/^decoded  :/decoded:/" | tr " " "\n" | sed -e "s/,$//" -e "/^$/d" -e "/^annotated/d" -e "/^decoded/d" | sort -t"/" -k2.2'
