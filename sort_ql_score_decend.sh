# Written for sorting query likelihood file by ql-value in decend.

mkdir -p /media/zzh/HDD1/clueweb09_querylikelihood_sorted

for i in `ls /media/zzh/HDD1/clueweb09_querylikelihood/`
do
    sort  -r -t ',' -g -k 2  /media/zzh/HDD1/clueweb09_querylikelihood/$i > /media/zzh/HDD1/clueweb09_querylikelihood_sorted/sort.`basename $i`
    
done
