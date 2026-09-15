# work-log

`work-log` is a command-line app that logs how long you have been working on something.
It was made to be manually shut down to force you to be aware of when you
context switch. It is split between `tasks` and `categories`. `categories` have many
`tasks`, but `tasks` can only have one `category`.

## Setup

It's best if you alias `java -jar path/to/jar/file` as `work-log`.

## Build

To build using clj tools:
```bash
clj -T:build uber
```

## Tests
To run tests using clj tools:
```bash
clj -M:test
```

## Usages

```bash
work-log list category
work-log list task
work-log add category CATEGORY
work-log add task TASK {CATEGORY}
work-log edit task TASK --category CATEGORY
work-log edit task TASK --name NEW_TASK_NAME
work-log edit category CATEGORY --name NEW_CATEGORY_NAME
work-log start TASK
```

Can also be run with clj:
```bash
clj -M -m work-log.main
```