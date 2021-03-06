package cljukebox;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class AudioTrackScheduler extends AudioEventAdapter {

  public final List<AudioTrack> queue;
  private final AudioPlayer player;
  public boolean loop = false;

  public AudioTrackScheduler(final AudioPlayer player) {
    queue = Collections.synchronizedList(new LinkedList<>());
    this.player = player;
  }

  public List<AudioTrack> getQueue() {
    return queue;
  }

  public AudioTrack nowPlaying() {
    return player.getPlayingTrack();
  }

  public boolean play(final AudioTrack track) {
    return play(track, false);
  }

  public boolean play(final AudioTrack track, final boolean force) {
    final boolean playing = player.startTrack(track, !force);

    if (!playing) {
      queue.add(track);
    }

    return playing;
  }

  public boolean skip() {
    player.stopTrack();
    if (queue.isEmpty()) {
      return true;
    } else {
      return play(queue.remove(0), false);
    }
  }

  public boolean switchLoop() {
    loop = !loop;
    return loop;
  }

  public boolean clear() {
    queue.clear();
    return skip();
  }

  public boolean shuffle() {
    Collections.shuffle(queue);
    return true;
  }

  @Override
  public void onTrackEnd(final AudioPlayer player, final AudioTrack track, final AudioTrackEndReason endReason) {
    if(loop && endReason == AudioTrackEndReason.FINISHED) {
	play(track.makeClone(), true);
    } else {
      skip();
    }
  }
}
