File-Based Queue
===

Storage Design
---

* *LogIndex*
  - read offset
  - write offset
  - read file number
  - write file number
  - size
* *LogEntity*
  - next file number
  - end offset

Interface
---

* *FSQueue*
  - void add(byte[] message, int offset, int length)
  - byte[] readNextAndRemove()

Threading Model
---

* Every FQueue has a thread
  - pre-create next file
  - delete consumed file
* At most 2 LogEntity threads
  - fsync
