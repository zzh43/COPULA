#!/bin/sh

if [ $# -ne 2 ]; then
    echo "Usage: $0  path_to_trec2011_bm25_before  path_to_trec2011_bm25_after" 1>&2
    exit 1
fi

before=$1  ##Like "/mnt/diska/trec2011_bm25" 最後に"/"を付けないこと
after=$2  ##Like "/mnt/diska/trec2011_bm25_after" 最後に"/"を付けないこと

mkdir -p ${after}


for file in `ls ${before}/*.csv`
do
    sort -t ',' -k 1,1  $file   > ${after}/`basename ${file}`
    
done

##これが終わったら、beforeに指定した trec2011_bm25 を別の名前に、afterに指定した trec_2011_after を trec2011_bm25に名前を変更して下さい。

##処理が面倒であれば、BM25Calculator2.java プログラム中の出力ファイル名を trec2011_bm25 以外のものに変更して下さい。
