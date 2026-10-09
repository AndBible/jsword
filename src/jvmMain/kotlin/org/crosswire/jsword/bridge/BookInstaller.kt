/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *      http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * © CrossWire Bible Society, 2008 - 2016
 *
 */
/**
 * Command line programs to do useful things.
 */
package org.crosswire.jsword.bridge

import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookFilter
import org.crosswire.jsword.book.BookFilters
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.install.InstallException
import org.crosswire.jsword.book.install.InstallManager
import org.crosswire.jsword.book.install.Installer

/**
 * Exports the Book in SWORD's imp format. This is identical to SWORD's mod2imp.
 * Note: it does not work with GenBook.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class BookInstaller {

    private var installManager: InstallManager = InstallManager()

    /**
     * Uninstall a book.
     * 
     * @param book
     *            the book to delete
     * @throws BookException
     */
    @Throws(BookException::class)
    open fun deleteBook(book: Book?) {
        // Actually do the delete
        // This should be a call on installer.
        // This will also remove the book from the list of installed books.
        book!!.driver!!.delete(book)
    }

    /**
     * Get a list of all known installers.
     * 
     * @return the list of installers
     */
    open val installers: Map<String, Installer>
        get() =
            // Ask the Install Manager for a map of all known remote repositories
            // sites
            installManager.installers

    /**
     * Get a list of all known books for an installer.
     * 
     * @param repositoryName
     * @return the list of books at that repository
     */
    open fun getRepositoryBooks(repositoryName: String?): MutableList<Book> {
        return installManager.getInstaller(repositoryName)!!.getBooks()
    }

    /**
     * Get a list of books in a repository by BookFilter.
     * 
     * @param repositoryName 
     *            The name of the repository
     * @param filter
     *            The book filter
     * @return the matching books
     * @see BookFilter
     * @see Books
     */
    open fun getRepositoryBooks(repositoryName: String?, filter: BookFilter?): MutableList<Book> {
        return installManager.getInstaller(repositoryName)!!.getBooks(filter)
    }

    /**
     * Get a list of books in a repository by CustomFilter specification
     * 
     * @param repositoryName 
     *            the name of the repository
     * @param filterSpec
     *            The filter string
     * @return the list of books
     * @see BookFilters#getCustom(java.lang.String)
     * @see Books
     */
    open fun getRepositoryBooks(repositoryName: String?, filterSpec: String): MutableList<Book> {
        return getRepositoryBooks(repositoryName, BookFilters.getCustom(filterSpec))
    }

    /**
     * Get a particular installed book by initials.
     * 
     * @param repositoryName 
     *            the name of the repository
     * @param bookInitials
     *            The book name to search for
     * @return The found book. Null otherwise.
     */
    open fun getRepositoryBook(repositoryName: String?, bookInitials: String): Book? {
        return installManager.getInstaller(repositoryName)!!.getBook(bookInitials)
    }

    /**
     * Reload the local cache for a remote repository.
     * 
     * @param repositoryName
     * @throws InstallException
     */
    @Throws(InstallException::class)
    open fun reloadBookList(repositoryName: String?) {
        installManager.getInstaller(repositoryName)!!.reloadBookList()
    }

    /**
     * Get a Book from the repository. Note this does not install it.
     * 
     * @param repositoryName
     *            the repository from which to get the book
     * @param bookName
     *            the name of the book to get
     * @return the Book
     */
    open fun getBook(repositoryName: String?, bookName: String): Book? {
        return installManager.getInstaller(repositoryName)!!.getBook(bookName)
    }

    /**
     * Install a book, overwriting it if the book to be installed is newer.
     * 
     * @param repositoryName
     *            the name of the repository from which to get the book
     * @param book
     *            the book to get
     * @throws BookException
     * @throws InstallException
     */
    @Throws(BookException::class, InstallException::class)
    open fun installBook(repositoryName: String?, book: Book?) {
        // An installer knows how to install books
        val installer = installManager.getInstaller(repositoryName)

        // Delete the book, if present
        // At the moment, JSword will not re-install. Later it will, if the
        // remote version is greater.
        if (Books.installed().getBook(book!!.initials) != null) {
            deleteBook(book)
        }

        // Now install it. Note this is a background task.
        installer!!.install(book)
    }

    companion object {
        /**
         * Get a list of all installed books.
         * 
         * @return the list of installed books
         */
        @JvmStatic
        val installedBooks: MutableList<Book>
            get() = Books.installed().getBooks()

        /**
         * Get a list of installed books by BookFilter.
         * 
         * @param filter
         *            The book filter
         * @return the list of matching books
         * @see BookFilter
         * @see Books
         */
        @JvmStatic
        fun getInstalledBooks(filter: BookFilter?): MutableList<Book> {
            return Books.installed().getBooks(filter)
        }

        /**
         * Get a list of books by CustomFilter specification
         * 
         * @param filterSpec
         *            The filter string
         * @return the list of matching books
         * @see BookFilters#getCustom(java.lang.String)
         * @see Books
         */
        @JvmStatic
        fun getInstalledBooks(filterSpec: String): MutableList<Book> {
            return getInstalledBooks(BookFilters.getCustom(filterSpec))
        }

        /**
         * Get a particular installed book by initials.
         * 
         * @param bookInitials
         *            The book name to search for
         * @return The found book. Null otherwise.
         */
        @JvmStatic
        fun getInstalledBook(bookInitials: String?): Book? {
            return Books.installed().getBook(bookInitials)
        }

        /**
         * BookInstaller can manage the installation of books with the following
         * capabilities.
         * 
         * <p>Usage: BookInstaller [option]</p>
         * <table border="0">
         * <caption>Options</caption>
         * <tr>
         * <td>uninstall</td>
         * <td>bookName</td>
         * <td>Uninstall book</td>
         * </tr>
         * <tr>
         * <td>sources</td>
         * <td>&nbsp;</td>
         * <td>List source repositories</td>
         * </tr>
         * <tr>
         * <td>list</td>
         * <td>&nbsp;</td>
         * <td>List installed books</td>
         * </tr>
         * <tr>
         * <td>list</td>
         * <td>repositoryName</td>
         * <td>list available books from a repository</td>
         * </tr>
         * <tr>
         * <td>reload</td>
         * <td>repositoryName</td>
         * <td>Reload the local cache for a repository</td>
         * </tr>
         * <tr>
         * <td>install</td>
         * <td>repositoryName bookName</td>
         * <td>Install a book from a repository</td>
         * </tr>
         * </table>
         * 
         * @param args
         */
        @JvmStatic
        fun main(args: Array<String>) {
            if (args.size < 1) {
                usage()
                return
            }

            System.err.print("BookInstaller")
            for (i in args.indices) {
                System.err.print(' ')
                System.err.print(args[i])
            }
            System.err.print('\n')

            val installer = BookInstaller()

            val operation = args[0]
            if ("uninstall".equals(operation, ignoreCase = true)) {
                if (args.size == 2) {
                    val b = Books.installed().getBook(args[1])
                    if (b == null) {
                        System.err.println("Book not found")
                        return
                    }
                    try {
                        installer.deleteBook(b)
                    } catch (e: BookException) {
                        e.printStackTrace()
                    }
                } else {
                    usage()
                }
            } else if ("sources".equals(operation, ignoreCase = true)) {
                // Get all the installers one after the other
                val installers = installer.installers
                for (name in installers.keys) {
                    System.out.println(name)
                }
            } else if ("list".equals(operation, ignoreCase = true)) {
                if (args.size == 1) {
                    for (book in BookInstaller.installedBooks) {
                        System.out.println(book.initials)
                    }
                } else if (args.size == 2) {
                    for (book in installer.getRepositoryBooks(args[1])) {
                        System.out.println(book.initials)
                    }
                } else {
                    usage()
                }
            } else if ("reload".equals(operation, ignoreCase = true)) {
                if (args.size == 2) {
                    try {
                        installer.reloadBookList(args[1])
                    } catch (e: InstallException) {
                        e.printStackTrace()
                    }
                } else {
                    usage()
                }
            } else if ("install".equals(operation, ignoreCase = true)) {
                if (args.size == 3) {
                    val b = installer.getBook(args[1], args[2])
                    if (b == null) {
                        System.err.println("Book not found")
                        return
                    }
                    try {
                        installer.installBook(args[1], b)
                    } catch (e: BookException) {
                        e.printStackTrace()
                    } catch (e: InstallException) {
                        e.printStackTrace()
                    }
                } else {
                    usage()
                }
            } else {
                usage()
            }
        }

        @JvmStatic
        fun usage() {
            System.err.println("usage: BookInstaller <option>")
            System.err.println("Options:")
            System.err.println("    uninstall bookName                 Uninstall book")
            System.err.println("    sources                            List remote source repositories")
            System.err.println("    list                               List installed books")
            System.err.println("    list      repositoryName           List available books from a repository")
            System.err.println("    reload    repositoryName           Reload local cache for a repository")
            System.err.println("    install   repositoryName bookName  Install a book from a repository")
        }
    }
}
