package com.android.perrier1034.post_it_note.util

object SeqUtil {

  def zipMap[T, U, V](list1: Seq[T], list2: Seq[U])(f: Function2[T, U, V]) = {
    if (list1.isEmpty)throw new RuntimeException("list1.isEmpty")
    if ( list1.size != list2.size) throw new RuntimeException("list1.size != list2.size")

    val iter1 = list1.iterator
    val iter2 = list2.iterator

    var dst = List[V]()
    while (iter1.hasNext) dst = f(iter1.next(), iter2.next()) :: dst

    dst.reverse.toSeq
  }

  def zipEach[T, U](list1: Seq[T], list2: Seq[U])(f: Function2[T, U, Unit]) = {
    if (list1.isEmpty ) throw new RuntimeException("list1.isEmpty")
    if ( list1.size != list2.size) throw new RuntimeException("list1.size != list2.size")

    val iter1 = list1.iterator
    val iter2 = list2.iterator

    while (iter1.hasNext) f(iter1.next(), iter2.next())
  }

}
