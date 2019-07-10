/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.0
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.equeim.libtremotesf;

public class TorrentFile {
  private transient long swigCPtr;
  private transient boolean swigCMemOwn;

  protected TorrentFile(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(TorrentFile obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void swigSetCMemOwn(boolean own) {
    swigCMemOwn = own;
  }

  @SuppressWarnings("deprecation")
  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        libtremotesfJNI.delete_TorrentFile(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public StringsVector getPath() {
    long cPtr = libtremotesfJNI.TorrentFile_path_get(swigCPtr, this);
    return (cPtr == 0) ? null : new StringsVector(cPtr, false);
  }

  public long getSize() {
    return libtremotesfJNI.TorrentFile_size_get(swigCPtr, this);
  }

  public long getCompletedSize() {
    return libtremotesfJNI.TorrentFile_completedSize_get(swigCPtr, this);
  }

  public boolean getWanted() {
    return libtremotesfJNI.TorrentFile_wanted_get(swigCPtr, this);
  }

  public int getPriority() {
    return libtremotesfJNI.TorrentFile_priority_get(swigCPtr, this);
  }

  public boolean getChanged() {
    return libtremotesfJNI.TorrentFile_changed_get(swigCPtr, this);
  }

  public final static class Priority {
    public final static int LowPriority = -1;
    public final static int NormalPriority = LowPriority + 1;
    public final static int HighPriority = NormalPriority + 1;
  }

}
