package org.nuist.rpc.example.netty.example_1;


import org.nuist.rpc.common.exception.remoting.RemotingCommmonCustomException;
import org.nuist.rpc.common.transport.body.CommonCustomBody;
import org.nuist.rpc.remoting.model.RemotingTransporter;

import java.util.ArrayList;
import java.util.List;

public class NettyTransporterTest1 {
	
	public static final byte STUDENTS = 1;
	public static final byte TEACHER = 2;
	
	public static void main(String[] args) {
		
		
		StudentInfos infos = new StudentInfos();
		//学生信息
		@SuppressWarnings("unused")
		RemotingTransporter studentsRemotingTransporter = RemotingTransporter.createRequestTransporter(STUDENTS, infos);
		//学生信息
		TeacherInfo info = new TeacherInfo();
		@SuppressWarnings("unused")
		RemotingTransporter teacherRemotingTransporter = RemotingTransporter.createRequestTransporter(TEACHER, info);
	}

	private static class StudentInfos implements CommonCustomBody {

		List<String> students = new ArrayList<String>();

		@Override
		public void checkFields() throws RemotingCommmonCustomException {
		}

		public List<String> getStudents() {
			return students;
		}

		public void setStudents(List<String> students) {
			this.students = students;
		}

	}

	private static class TeacherInfo implements CommonCustomBody {

		String teacher = "";

		@Override
		public void checkFields() throws RemotingCommmonCustomException {
		}

		public String getTeacher() {
			return teacher;
		}

		public void setTeacher(String teacher) {
			this.teacher = teacher;
		}

	}

}
