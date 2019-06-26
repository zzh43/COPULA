# Witten for sorting the query bm25 file by bm25 value decend.

mkdir -p /media/zzh/HDD1/clueweb09_query_bm25_sorted

for i in `ls /media/zzh/HDD1/clueweb09_query_bm25/`
do
    sort -r -t ',' -g -k 2  /media/zzh/HDD1/clueweb09_query_bm25/$i > /media/zzh/HDD1/clueweb09_query_bm25_sorted/sort.`basename $i`
    
done
