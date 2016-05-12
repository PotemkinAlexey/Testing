package com.springapp.mvc.controller;

import com.springapp.mvc.domain.AnswersEntity;
import com.springapp.mvc.domain.AnswersUserEntity;
import com.springapp.mvc.domain.QuestionsEntity;
import com.springapp.mvc.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class QuestionController {

    @Autowired
    private QuestionRepository questionRepository;

    private String returnCode(String parent) {
        String ID = parent.substring(5);
        int pos = ID.indexOf(" ");
        return ID.substring(0, pos);
    }

    @PreAuthorize("hasRole('admin')")
    @RequestMapping(value = "/writequestion", method = RequestMethod.POST, produces = {"text/html; charset=UTF-8"})
    @ResponseBody
    public String writeQuestion(@RequestParam("category") String category, @RequestParam("question") String question, @RequestParam("code") String code, @RequestParam("typeq") int typeq) {

        int categoryID;
        if (!category.equals("")) {
            categoryID = new Integer(returnCode(category));
        } else {
            categoryID = 0;
        }

        QuestionsEntity ourQuestion;

        if (!code.equals("")) {
            ourQuestion = questionRepository.getQuestionByID(new Integer(code));
        } else {
            ourQuestion = new QuestionsEntity();
        }

        ourQuestion.setTypeQuestion(typeq);

        ourQuestion.setQuestion(question);
        ourQuestion.setCategory(categoryID);

        try {
            if (!code.equals("")) {

                List<AnswersEntity> listAnswers=questionRepository.getAnswersByQuestion(ourQuestion.getId());

                if (listAnswers.size()>1 && typeq==3) {
                    return "Ошибка! Проверьте тип вопроса и кол-во ответов";
                }else if (listAnswers.size()==1 && typeq==3){
                    AnswersEntity ourElement=listAnswers.get(0);
                    ourElement.setCorrect((byte)1);
                    questionRepository.updateAnswer(ourElement);

                }else if(listAnswers.size()>1 && typeq==1) {
                    int countAnswers = 0;
                    for (AnswersEntity ourElement : listAnswers) {
                        if (ourElement.getCorrect() == 1) {
                            countAnswers++;
                        }
                    }
                    if (countAnswers>1) {
                        return "Ошибка! Проверьте тип вопроса и кол-во правильных ответов";
                    }
                }

                questionRepository.updateQuestion(ourQuestion);
            } else {
                questionRepository.createQuestion(ourQuestion);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        QuestionsEntity newOurQuestion = questionRepository.getQuestionByText(ourQuestion.getQuestion());

        StringBuilder ourAnswer = new StringBuilder(10);
        ourAnswer.append(newOurQuestion.getId());
        return ourAnswer.toString();
    }

    @PreAuthorize("hasRole('admin')")
    @RequestMapping(value = "/createtree", method = RequestMethod.POST, produces = {"text/html; charset=UTF-8"})
    @ResponseBody
    public String getTree(@RequestParam("code") int code, @RequestParam("context") String context){
        return createTable(code,context);
    }


    private String createTable(int idquestion, String context){
        List<AnswersEntity> ourQuestion = questionRepository.getAnswersByQuestion(idquestion);
        StringBuilder ourTable = new StringBuilder(200);

        ourTable.append(" <thead> ");
        ourTable.append("<tr> ");
        ourTable.append("<th>Ответ</th> ");
        ourTable.append("<th>Флаг ответа</th> ");
        ourTable.append("<th>Редактировать</th> ");
        ourTable.append("<th>Удалить</th> ");
        ourTable.append("</tr> ");
        ourTable.append("</thead> ");

        ourTable.append("<tbody> ");
        for (AnswersEntity ourElement : ourQuestion) {

            ourTable.append(" <td>");
            ourTable.append(ourElement.getAnswer());
            ourTable.append(" </td> ");

            ourTable.append(" <td> ");
            if (ourElement.getCorrect() == 1) {
                ourTable.append(" <span class=\"label bg-success\" style=\"background: #00cc00\"> ");
                ourTable.append("Правильный");
            } else if (ourElement.getCorrect() == 0) {
                ourTable.append(" <span class=\"label bg-danger\" style=\"background: red\"> ");
                ourTable.append("Неправильный");
            }
            ourTable.append(" </span>");
            ourTable.append(" </td>");


            ourTable.append(" <td>");

            ourTable.append(" <span class=\"tooltip-area\">");
            ourTable.append(" <a href=\"");
            ourTable.append(context);
            ourTable.append("/editqanswer/");
            ourTable.append(ourElement.getId());
            ourTable.append("\"");

            ourTable.append(" class=\"label btn-info\"> <i class=\"fa fa-pencil\">Редактировать</i> </a>");
            ourTable.append(" </span>");
            ourTable.append(" </td>");

            ourTable.append(" <td>");
            ourTable.append(" <span class=\"tooltip-area\">");
            ourTable.append(" <a href=\"");
            ourTable.append("javascript:fundelanswer(");
            ourTable.append(ourElement.getId());
            ourTable.append(",");
            ourTable.append(ourElement.getIdQuestion());
            ourTable.append(")\"");
            ourTable.append(" class=\"label btn-danger\"> <i class=\"fa fa-trash\">Удалить</i></a>");
            ourTable.append(" </span>");
            ourTable.append(" </td>");

            ourTable.append(" </tr>");


        }
        ourTable.append(" </tbody>");
        return ourTable.toString();
    }


    @PreAuthorize("hasRole('admin')")
    @RequestMapping(value = "/writeanswer", method = RequestMethod.POST, produces = {"text/html; charset=UTF-8"})
    @ResponseBody
    public String writeAnswer(@RequestParam("context") String context, @RequestParam("answer") String answer, @RequestParam("codequestion") int codequestion, @RequestParam("flag") int flag, @RequestParam("typeq") int typeq, @RequestParam("answerid") String answerid) throws Exception {
        //check type of question in DB
        QuestionsEntity ourQuestion = questionRepository.getQuestionByID(codequestion);
        if (ourQuestion.getTypeQuestion() != typeq) //write question type
        {
            ourQuestion.setTypeQuestion(typeq);
            try {
                questionRepository.updateQuestion(ourQuestion);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<AnswersEntity> myAnswers = questionRepository.getAnswersByQuestion(codequestion);

        StringBuilder error=new StringBuilder(100);

        if (typeq==3 && myAnswers.size()>0){
            error.append("Не может быть несколько ответов при таком типе вопроса");
//            System.out.print(error);
            return error.toString();
        }

        AnswersEntity ourAnswer;
        if (!answerid.equals("")) {
            ourAnswer = questionRepository.getAnswersByID(new Integer(answerid));
        } else {
            ourAnswer = new AnswersEntity(); //empty entities
        }
            ourAnswer.setCorrect((byte) flag);
            ourAnswer.setAnswer(answer);
            ourAnswer.setIdQuestion(codequestion);
        if (ourAnswer.getId() == 0)//answer not found
        {
            questionRepository.createAnswer(ourAnswer);
        }
        else
        {
            questionRepository.updateAnswer(ourAnswer);
        }

        if ((typeq == 1) && (flag == 1)) //we should check and rewrite, only one correct answer in base must be, because flag=1
        {
            for (AnswersEntity ourElement : myAnswers) {
                if (ourElement.getCorrect() == 1) {
                    ourElement.setCorrect((byte) 0);
                    questionRepository.updateAnswer(ourElement);
                }
            }
        }

        return createTable(codequestion,context);
    }


    @PreAuthorize("hasRole('admin')")
    @RequestMapping(value = "/delanswer", method = RequestMethod.POST, produces = {"text/html; charset=UTF-8"})
    @ResponseBody
    public String delanswer(@RequestParam("context") String context, @RequestParam("idanswer") int idanswer, @RequestParam("idquestion") int idquestion) throws Exception {

        List<AnswersUserEntity> ourAnswers=questionRepository.getAnswerUserByQuestionID(idquestion);

        if (ourAnswers.size()>0){
            return "error";
        }

        AnswersEntity answer=questionRepository.getAnswersByID(idanswer);
        questionRepository.deleteAnswer(answer);
        return createTable(idquestion,context);
    }


    @PreAuthorize("hasRole('admin')")
    @RequestMapping(value = "/editquestion/{id}/{context}", method = RequestMethod.GET)
    public String addQuestion(@PathVariable int id, @PathVariable String context, Model model) {
        QuestionsEntity ourQuestion = questionRepository.getQuestionByID(id);
        context=context.substring(1);

        StringBuilder ourCategory = new StringBuilder(20);
        ourCategory.append(" Код:");
        ourCategory.append(ourQuestion.getCategoryById().getId());
        ourCategory.append(" ");
        ourCategory.append(ourQuestion.getCategoryById().getCategory());

        model.addAttribute("category", ourCategory.toString());
        model.addAttribute("code", ourQuestion.getId());
        model.addAttribute("questiontext", ourQuestion.getQuestion());
        model.addAttribute("questiontype", ourQuestion.getTypeQuestion());
        model.addAttribute("table",createTable(id,context));

        return "addquestion";
    }


}
