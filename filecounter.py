# -*- coding: utf-8 -*-

import os.path

DIR = '/media/zzh/HDD1/clueweb09_html'


def main():
    file_count = sum(len(files) for _, _, files in os.walk(DIR))
    print(file_count)


if __name__ == '__main__':
    main()
