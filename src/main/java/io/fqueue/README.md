File-Based Queue
===

Another simple, fast and persistent queue based on memory mapped file.

Why another? there are existing implements like

* [*BigQueue*](https://github.com/bulldog2011/bigqueue)
* [*BigQueue fork*](https://github.com/maxim5/bigqueue)
* [*Chronicle Queue*](https://github.com/OpenHFT/Chronicle-Queue)

For this one, the original idea and source code came from [fqueue](https://code.google.com/p/fqueue/). But then it was enhanced and productionized.

Storage Design
---

There are 2 kind of files.

* *LogIndex*
  - read offset
  - write offset
  - read file number
  - write file number
  - size
* *LogEntity*
  - next file number
  - end offset

LogEntities are linked by next file number. At any time there are at most two instances of memory mapped LogEntity, one is the writer while the other is the reader. When the reader catches up old LogEntity are deleted.

Interface
---

* *FSQueue*
  - void add(byte[] message, int offset, int length)
  - byte[] readNextAndRemove()

* *FQueue*
  - a facade implements `Queue<byte[]>`

Threading Model
---

* Every FQueue has a thread
  - pre-create next file
  - delete consumed file
* At most 2 LogEntity threads
  - fsync

So there could be at most 3 platform threads which are doing system IO in the background.

Off-heap Memory
---

At most 2 LogEntity files are mapped into memory. The size of this file is configurable.
