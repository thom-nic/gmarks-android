/* This file is part of GMarks. Copyright 2011 Thom Nichols
 *
 * GMarks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GMarks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GMarks.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thomnichols.android.gmarks;

class IteratorException extends RuntimeException {
	private static final long serialVersionUID = -5283100944881775242L;
	public IteratorException() { super(); }
	public IteratorException(String arg0, Throwable arg1) { super(arg0, arg1); }
	public IteratorException(String arg0) { super(arg0); }
	public IteratorException(Throwable arg0) { super(arg0); }
}