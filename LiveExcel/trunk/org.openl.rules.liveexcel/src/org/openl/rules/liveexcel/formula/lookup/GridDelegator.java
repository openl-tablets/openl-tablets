package org.openl.rules.liveexcel.formula.lookup;

/**
 * 
 * Represents table like
 * <table>
 * <tr><td></td><td></td><td>A</td><td>A</td><td>B</td><td>B</td></tr>
 * <tr><td></td><td></td><td>A1</td><td>A2</td><td>B1</td><td>B2</td></tr>
 * <tr><td>C</td><td>C1</td><td>1</td><td>2</td><td>3</td><td>4</td></tr>
 * <tr><td>C</td><td>C2</td><td>5</td><td>6</td><td>7</td><td>8</td></tr>
 * <tr><td>C</td><td>C3</td><td>9</td><td>10</td><td>11</td><td>12</td></tr>
 * <tr><td>D</td><td>D1</td><td>13</td><td>14</td><td>15</td><td>16</td></tr>
 * <tr><td>D</td><td>D2</td><td>17</td><td>18</td><td>19</td><td>20</td></tr>
 * </table>
 * 
 * as table like
 * <table>
 * <tr><td>A</td><td>A1</td><td>C</td><td>C1</td><td>1</td></tr>
 * <tr><td>A</td><td>A1</td><td>C</td><td>C2</td><td>5</td></tr>
 * <tr><td>A</td><td>A1</td><td>C</td><td>C3</td><td>9</td></tr>
 * <tr><td>A</td><td>A1</td><td>D</td><td>D1</td><td>13</td></tr>
 * <tr><td>A</td><td>A1</td><td>D</td><td>D1</td><td>17</td></tr>
 *
 * <tr><td>A</td><td>A2</td><td>C</td><td>C1</td><td>2</td></tr>
 * <tr><td>A</td><td>A2</td><td>C</td><td>C2</td><td>6</td></tr>
 * <tr><td>A</td><td>A2</td><td>C</td><td>C3</td><td>10</td></tr>
 * <tr><td>A</td><td>A2</td><td>D</td><td>D1</td><td>14</td></tr>
 * <tr><td>A</td><td>A2</td><td>D</td><td>D1</td><td>18</td></tr>
 *
 * <tr><td>B</td><td>B1</td><td>C</td><td>C1</td><td>3</td></tr>
 * <tr><td>B</td><td>B1</td><td>C</td><td>C2</td><td>7</td></tr>
 * <tr><td>B</td><td>B1</td><td>C</td><td>C3</td><td>11</td></tr>
 * <tr><td>B</td><td>B1</td><td>D</td><td>D1</td><td>15</td></tr>
 * <tr><td>B</td><td>B1</td><td>D</td><td>D1</td><td>19</td></tr>
 * 
 * <tr><td>B</td><td>B2</td><td>C</td><td>C1</td><td>4</td></tr>
 * <tr><td>B</td><td>B2</td><td>C</td><td>C2</td><td>8</td></tr>
 * <tr><td>B</td><td>B2</td><td>C</td><td>C3</td><td>12</td></tr>
 * <tr><td>B</td><td>B2</td><td>D</td><td>D1</td><td>16</td></tr>
 * <tr><td>B</td><td>B2</td><td>D</td><td>D1</td><td>20</td></tr>
 * </table>
 * 
 * 
 * @author spetrakovsky
 *
 */
public class GridDelegator extends Grid {

	private Grid grid;

	private int thh;

	private int thw;

	private int tlw;

	private int tlh;

	public GridDelegator(Grid grid) {
		this.grid = grid;
		initialize();
	}

	private void initialize() {
		int x = 0;
		for (; x < grid.getWidth(); x ++) {
			if (!grid.getValue(x, 0).equals("")) {
				break;
			}
		}
		thw = grid.getWidth() - x;
		tlw = grid.getWidth() - thw;
		
		int y = 0;
		for (; y < grid.getHeight(); y ++) {
			if (!grid.getValue(0, y).equals("")) {
				break;
			}
		}
		tlh = grid.getHeight() - y;
		thh = grid.getHeight() - tlh;
	}

	@Override
	public int getHeight() {
		return tlh * thw;
	}

	@Override
	public int getWidth() {
		return thh + tlw + 1;
	}

	@Override
	public String getValue(int x, int y) {
		return grid.getValue(getX(x, y), getY(x, y));
	}

	private int getX(int x, int y) {
		if (x < thh) {
			return tlw + (int)(y / tlh);
		} else if (x < tlw + thh) {
			return x - thh;
		} else {
			return tlw + (int)(y / tlh);
		}
	}

	private int getY(int x, int y) {
		if (x < thh) {
			return x;
		} else if (x < tlw + thh) {
			return thh + y % tlh;
		} else {
			return thh + y % tlh;
		}
	}

}
