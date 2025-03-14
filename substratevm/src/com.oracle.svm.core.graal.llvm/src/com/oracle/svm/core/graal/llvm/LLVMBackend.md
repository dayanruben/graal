# LLVM Backend for Native Image

Native Image provides an alternative backend that uses the [LLVM intermediate representation](https://llvm.org/docs/LangRef.html) and the [LLVM compiler](http://llvm.org/docs/CommandGuide/llc.html) to produce native executables.
This LLVM backend enables users to [target alternative architectures](#how-to-add-a-target-architecture-to-graalvm-using-llvm-backend) that are not directly supported by GraalVM Native Image. However, this approach introduces some performance costs.

## Installing and Usage

The LLVM Backend is not included by default as part of Native Image. To use it, you need to <a href="https://github.com/oracle/graal/blob/master/vm/README.md" target="_blank">build GraalVM from source</a> using the following <a href="https://github.com/graalvm/mx/" target="_blank">mx</a> commands:
```shell
mx --dynamicimports /substratevm build
export JAVA_HOME=$(mx --dynamicimports /substratevm graalvm-home)
```

To enable the LLVM backend, pass the `-H:CompilerBackend=llvm` option to the `native-image` command. 

## Code Generation Options

* `-H:+BitcodeOptimizations`: enables aggressive optimizations at the LLVM bitcode level. This is experimental and may cause bugs.

## Debugging Options

* `-H:TempDirectory=`: specifies where the files generated by Native Image will be saved. The LLVM files are saved under `SVM-<timestamp>/llvm` in this directory.
* `-H:LLVMMaxFunctionsPerBatch=`: specifies the maximum size of a compilation batch\*. Setting it to 1 compiles every function separately, 0 compiles everything as a single batch.
* `-H:DumpLLVMStackMap=`: specifies a file in which to dump debugging information, including a mapping between compiled functions and the name of the corresponding bitcode file.

*About batches*: LLVM compilation happens in four phases:
1. LLVM bitcode files (named `f0.bc`, `f1.bc`, etc.,) are created for each function.
2. The bitcode files are linked into batches (named `b0.bc`, `b1.bc`, etc.). This phase is skipped when `-H:LLVMMaxFunctionsPerBatch=1` is specified.
3. The batches are optimized (into `b0o.bc`, `b1o.bc`, etc.,) and then compiled (into `b0.o`, `b1.o`, etc.).
4. The compiled batches are linked into a single object file (`llvm.o`), which is then linked into the final executable.

## How to Add a Target Architecture to GraalVM Using LLVM Backend

An interesting use case for the LLVM backend is to target a new architecture without having to implement a completely new backend for Native Image.
The following are the necessary steps to achieve this at the moment.

### Target-Specific LLVM Settings

There are a few instances where the GraalVM code has to go deeper than the target-independent nature of LLVM.
These are most notably inline assembly snippets to implement direct register accesses and direct register jumps (for trampolines), as well as precisions about the structure of the stack frames of the code emitted by LLVM.
This represents less than a dozen simple values to be set for each new target.

_([Complete set of values for AArch64](https://github.com/oracle/graal/commit/80cceec6f6299181d94e844eb22dffbef3ecc9e4))_

### LLVM Statepoint Support

While the LLVM backend uses mostly common, well-supported features of LLVM, garbage collection support implies the use of statepoint intrinsics, an experimental feature of LLVM.
This means that supporting a new architecture will require the implementation of statepoints in LLVM for the requested target.
As most of the statepoint logic is handled at the bitcode level (that is, at a target-independent stage), this is mostly a matter of emitting the right type of calls to lower the statepoint intrinsics.

_([Implementation of statepoints for AArch64](https://reviews.llvm.org/D66012))_

### Object File Support

The data section for programs created with the LLVM backend of the Graal compiler is emitted independently from the code, which is handled by LLVM.
This means that the Graal compiler needs an understanding of object file relocations for the target architecture to be able to link the LLVM-compiled code with the GraalVM-generated data section.

_(see `ELFMachine$ELFAArch64Relocations` for an example)_