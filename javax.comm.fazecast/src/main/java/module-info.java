module javax.comm.fazecast {
	requires transitive javax.comm.basic;
	uses javax.comm.CommDriver;
	provides javax.comm.CommDriver with
		javax.comm.fazecast.FazecastCommDriver;
}
