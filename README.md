Synopsis
========

There are two different applications inside this package.

Concurrent MMAP file text search
--------------------------------

The first one is a *grep* like utility which uses concurrent threads to search a text string in a file. It reads
 the file by memory mapping a file invoking the [mmap system call](http://man7.org/linux/man-pages/man2/mmap.2.html).


Memory pages are initially not loaded into the resident memory or the OS page cache. They are loaded as soon as the
 file start to be read by the page fault mechanism.

When no resident memory is available or the OS require to free some memory, pages can be evicted by selecting them
using the LRU (last recent use) mechanism. You can verify the resident memory size within the /proc/[pid]/smaps file.

Simple Object database
----------------------

This application writes a java object to disk, using its own file format. It also ensures the filesystem block
 alignement writing 8K size blocks in the disk.

It also uses BTree ([red and black trees](https://en.wikipedia.org/wiki/Red%E2%80%93black_tree)) indexes for indexing
 the record fields. the indexes store a reference to the disk block on the file where the record resides.

I decided to encode each record using Base64. I'm not sure if this is ideal, but I was trying to avoid encoding
 issues. I believe can be improved for performance reasons.

The database is for now just capable of inserting and updating object records. The updates can occur in place
 if the updated document is equal or smaller than the original. This means the original blocks are modified and the
 indexes updated with the references.

Updates can also require to move the record to different blocks when the updated document is bigger. This will
 also update the indexes with the new references.

The update process can create some fragmentation in the file. A database defragmentation tool will be useful to
 help with this issue.

Additionally, there are two major things to do in order to improve the performance and the data consistency:

* Resident memory page cache (blocks)
* Journal mechanism and data checkpoints


Motivation
==========

The main goals were learning and having fun.

Installation
============

This is a Maven project, so all you need to do is running Maven. I also include some plugins for generating an
 executable JAR file and run the application.

Please run:

    mvn package

Tests
=====

Unit tests will reun with the Maven execution.

Contributors
============

For now it's jus my self. But anyone is inviting for having fun here.

License
=======

This project is licensed under GPL version 3.