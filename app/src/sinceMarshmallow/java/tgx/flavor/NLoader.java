package tgx.flavor;

import io.relimus.flatgram.BuildConfig;
import io.relimus.flatgram.N;

public class NLoader {
  private static boolean loaded;

  public static String collectLog () {
    return "";
  }

  public static synchronized boolean loadLibraries () {
    if (loaded) {
      return true;
    }
    if (BuildConfig.SHARED_STL) {
      System.loadLibrary("c++_shared");
    }
    System.loadLibrary("cryptox");
    System.loadLibrary("sslx");
    System.loadLibrary("tdjni");
    System.loadLibrary("leveldbjni");
    System.loadLibrary("tgcallsjni");
    System.loadLibrary("tgxjni");
    N.setupLibraries();
    loaded = true;
    return true;
  }
}
