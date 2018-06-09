/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.12
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.equeim.libtremotesf;

public class TorrentPeersVector {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected TorrentPeersVector(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(TorrentPeersVector obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        libtremotesfJNI.delete_TorrentPeersVector(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public long size() {
    return libtremotesfJNI.TorrentPeersVector_size(swigCPtr, this);
  }

  public long capacity() {
    return libtremotesfJNI.TorrentPeersVector_capacity(swigCPtr, this);
  }

  public void reserve(long n) {
    libtremotesfJNI.TorrentPeersVector_reserve(swigCPtr, this, n);
  }

  public boolean isEmpty() {
    return libtremotesfJNI.TorrentPeersVector_isEmpty(swigCPtr, this);
  }

  public void clear() {
    libtremotesfJNI.TorrentPeersVector_clear(swigCPtr, this);
  }

  public void add(Peer x) {
    libtremotesfJNI.TorrentPeersVector_add(swigCPtr, this, Peer.getCPtr(x), x);
  }

  public Peer get(int i) {
    long cPtr = libtremotesfJNI.TorrentPeersVector_get(swigCPtr, this, i);
    return (cPtr == 0) ? null : new Peer(cPtr, true);
  }

  public void set(int i, Peer val) {
    libtremotesfJNI.TorrentPeersVector_set(swigCPtr, this, i, Peer.getCPtr(val), val);
  }

}